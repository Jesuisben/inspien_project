
package com.inspien_project.mapper;

import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.CharacterCodingException;
import java.util.regex.Pattern;
import lombok.Getter;
import lombok.AllArgsConstructor;
import org.xml.sax.InputSource;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;
import com.inspien_project.dto.OrderInputDto;

@Component
public class OrderXmlMapper {

    @Getter
    @AllArgsConstructor
    public static class ParseResult {
        private final List<OrderInputDto> orderList;
        private final int totalCount;
        private final int rejectedCount;
        private final int emptyHeaderCount;
    }

    public ParseResult parse(byte[] bytes) {
        if (bytes == null || bytes.length == 0 || bytes.length > 1_048_576) {
            throw new IllegalArgumentException("XML 크기 1바이트 이상 1MiB 이하여야 함");
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            var builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new DefaultHandler() {
                @Override public void error(SAXParseException error) throws SAXException { throw error; }
                @Override public void fatalError(SAXParseException error) throws SAXException { throw error; }
            });

            var document = builder.parse(new InputSource(new StringReader(readXml(bytes))));
            Map<String, Element> headers = new LinkedHashMap<>();
            var headerNodes = document.getElementsByTagNameNS("*", "HEADER");
            for (int index = 0; index < headerNodes.getLength(); index++) {
                Element header = (Element) headerNodes.item(index);
                String userId = getValue(header, "USER_ID");
                getValue(header, "NAME");
                getValue(header, "ADDRESS");
                if (!"N".equals(getStatus(header))) {
                    throw new IllegalArgumentException("신규 주문의 STATUS는 N이어야 함");
                }
                if (headers.putIfAbsent(userId, header) != null) {
                    throw new IllegalArgumentException("같은 USER_ID의 HEADER가 중복됨");
                }
            }
            if (headers.isEmpty()) throw new IllegalArgumentException("HEADER가 없음");
            List<OrderInputDto> result = new ArrayList<>();
            Set<String> usedHeaders = new HashSet<>();
            int rejectedCount = 0;
            var items = document.getElementsByTagNameNS("*", "ITEM");
            for (int index = 0; index < items.getLength(); index++) {
                Element item = (Element) items.item(index);
                String userId = getValue(item, "USER_ID");
                String itemId = getValue(item, "ITEM_ID");
                String itemName = getValue(item, "ITEM_NAME");
                String price = getValue(item, "PRICE");
                if (!price.matches("[0-9]+(?:\\.[0-9]+)?")) {
                    throw new IllegalArgumentException("PRICE는 양수여야함");
                }
                Element header = headers.get(userId);
                if (header == null) {
                    rejectedCount++;
                    continue;
                }
                result.add(new OrderInputDto(userId, itemId, getValue(header, "NAME"),
                        getValue(header, "ADDRESS"), itemName, price));
                usedHeaders.add(userId);
            }
            if (result.isEmpty()) throw new IllegalArgumentException("처리할 수 있는 ITEM");
            return new ParseResult(List.copyOf(result), items.getLength(), rejectedCount,
                    headers.size() - usedHeaders.size());
        } catch (IllegalArgumentException error) {
            throw error;
        } catch (Exception error) {
            throw new IllegalArgumentException("XML 구문 및 인코딩을 확인", error);
        }
    }

    private String readXml(byte[] bytes) throws CharacterCodingException {
        String prefix = new String(bytes, 0, Math.min(bytes.length, 256), StandardCharsets.ISO_8859_1);
        var declaration = Pattern.compile("^\\s*<\\?xml\\s+[^?]*encoding\\s*=\\s*['\"]([^'\"]+)['\"][^?]*\\?>")
                .matcher(prefix);
        String xml;
        if (declaration.find()) {
            xml = decode(bytes, Charset.forName(declaration.group(1)));
        } else {
            xml = decode(bytes, Charset.forName("EUC-KR"));
        }
        if (xml.startsWith("\uFEFF")) xml = xml.substring(1);

        var start = Pattern.compile("^(<\\?xml\\s+[^?]*\\?>)").matcher(xml);
        if (start.find()) {
            return start.group(1) + "<ROOT>" + xml.substring(start.end()) + "</ROOT>";
        }
        return "<ROOT>" + xml + "</ROOT>";
    }

    private String decode(byte[] bytes, Charset charset) throws CharacterCodingException {
        return charset.newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
    }

    private String getStatus(Element header) {
        return getChild(header, "STATUS").getTextContent().strip();
    }

    private String getValue(Element parent, String name) {
        String text = getChild(parent, name).getTextContent();
        if (text.isBlank() || text.indexOf('^') >= 0 || text.indexOf('\r') >= 0 || text.indexOf('\n') >= 0) {
            throw new IllegalArgumentException(name + "의 빈값 및 파일 구분 문자 확인 필요");
        }
        return text;
    }

    private Element getChild(Element parent, String name) {
        Element found = null;
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element element && name.equals(element.getLocalName())) {
                if (found != null || element.getElementsByTagName("*").getLength() != 0) {
                    throw new IllegalArgumentException(name + " 구조가 정상적이지 않음");
                }
                found = element;
            }
        }
        if (found == null) throw new IllegalArgumentException(name + " 필드가 없음");
        return found;
    }
}