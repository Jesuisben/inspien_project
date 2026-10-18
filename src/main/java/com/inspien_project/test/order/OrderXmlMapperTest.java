// XML의 고객 정보와 상품 정보를 USER_ID로 매핑해서 올바르게 변환되는지 테스트

package com.inspien_project.test.order;

import static com.inspien_project.test.common.OrderTestHelper.*;

import com.inspien_project.test.common.OrderXmlData;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import com.inspien_project.common.IdGenerator;
import com.inspien_project.mapper.OrderXmlMapper;

public class OrderXmlMapperTest {

    private final OrderXmlMapper orderXmlMapper = new OrderXmlMapper();

    private byte[] bytes(String xml) { return xml.getBytes(StandardCharsets.UTF_8); }

    // HEADER 하나와 ITEM 둘이 주문 두 행으로 바뀌는지 확인
    void testHeaderItems() {
        var orderList = orderXmlMapper.parse(bytes(OrderXmlData.xml)).getOrderList();
        assertEquals(2, orderList.size());
        assertEquals("홍길동", orderList.get(1).getName());
        assertEquals("ITEM2", orderList.get(1).getItemId());
    }

    // XML 선언대로 EUC-KR 바이트를 읽는지 확인
    void testEucKr() {
        String xml = OrderXmlData.xml.replace("UTF-8", "EUC-KR");
        assertEquals("청바지", orderXmlMapper.parse(xml.getBytes(Charset.forName("EUC-KR"))).getOrderList().getFirst().getItemName());
    }

    // 실제 형식처럼 선언 및 바깥 루트가 없는 EUC-KR 확인
    void testXmlFragments() {
        String xml = OrderXmlData.xml.replaceFirst("<\\?xml[^?]*\\?>", "")
                .replace("<ORDERS>", "").replace("</ORDERS>", "");
        var result = orderXmlMapper.parse(xml.getBytes(Charset.forName("EUC-KR")));
        assertEquals(2, result.getOrderList().size());
        assertEquals("청바지", result.getOrderList().getFirst().getItemName());
        // '징'의 EUC-KR 바이트는 UTF-8로도 유효하여 UTF-8 우선 추측 시 글자가 바뀔 수 있음.
        // 예외 주의
        String sample = "<HEADER><USER_ID>U</USER_ID><NAME>징</NAME><ADDRESS>징</ADDRESS><STATUS>N</STATUS></HEADER>"
                + "<ITEM><USER_ID>U</USER_ID><ITEM_ID>I</ITEM_ID><ITEM_NAME>징</ITEM_NAME><PRICE>1</PRICE></ITEM>";
        var order = orderXmlMapper.parse(sample.getBytes(Charset.forName("EUC-KR"))).getOrderList().getFirst();
        assertEquals("징", order.getName());
        assertEquals("징", order.getAddress());
        assertEquals("징", order.getItemName());
        assertThrows(IllegalArgumentException.class,
                () -> orderXmlMapper.parse(new byte[] {(byte) 0xFF}));
        assertThrows(IllegalArgumentException.class,
                () -> orderXmlMapper.parse((xml + "<broken>").getBytes(Charset.forName("EUC-KR"))));
        assertThrows(IllegalArgumentException.class,
                () -> orderXmlMapper.parse(bytes(OrderXmlData.xml.replace("version=\"1.0\"", "version=\"broken\""))));
    }

    // 가격 앞자리 0과 이름 공백 그대로 유지하는지 확인
    void testPriceAndName() {
        var orderList = orderXmlMapper.parse(bytes(OrderXmlData.xml.replace("21000", "021000").replace("홍길동", " 홍길동 "))).getOrderList();
        assertEquals("021000", orderList.getFirst().getPrice());
        assertEquals(" 홍길동 ", orderList.getFirst().getName());
    }

    // namespace가 있는 XML에서도 필드를 찾는지 확인
    // 올바르게 처리하는지 확인
    void testNamespace() {
        String xml = OrderXmlData.xml.replace("<ORDERS>", "<ORDERS xmlns='urn:orders'>");
        assertEquals(2, orderXmlMapper.parse(bytes(xml)).getOrderList().size());
    }

    // 관계 오류 하나는 제외하지만, 정상 주문과 제외 건수를 함께 반환하는지 확인
    void testMissingHeader() {
        String xml = OrderXmlData.xml.replaceFirst("<ITEM><USER_ID>USER1", "<ITEM><USER_ID>UNKNOWN");
        var result = orderXmlMapper.parse(bytes(xml));
        assertEquals(2, result.getTotalCount());
        assertEquals(1, result.getOrderList().size());
        assertEquals(1, result.getRejectedCount());
        assertEquals("ITEM2", result.getOrderList().getFirst().getItemId());
        String invalid = OrderXmlData.xml.replace("<ITEM><USER_ID>USER1", "<ITEM><USER_ID>UNKNOWN");
        assertThrows(IllegalArgumentException.class, () -> orderXmlMapper.parse(bytes(invalid)));
    }

    // 같은 USER_ID의 HEADER 중복을 거절하는지 확인
    void testDuplicateHeader() {
        String header = "<HEADER><USER_ID>USER1</USER_ID><NAME>김</NAME><ADDRESS>서울</ADDRESS><STATUS>N</STATUS></HEADER>";
        assertThrows(IllegalArgumentException.class, () -> orderXmlMapper.parse(bytes(OrderXmlData.xml.replace("</ORDERS>", header + "</ORDERS>"))));
    }

    // ITEM이 없는 HEADER는 주문을 만들지 않고 별도 경고 건수로 남기는지 확인
    void testHeaderWithoutItem() {
        String header = "<HEADER><USER_ID>USER2</USER_ID><NAME>김</NAME><ADDRESS>서울</ADDRESS><STATUS>N</STATUS></HEADER>";
        var result = orderXmlMapper.parse(bytes(OrderXmlData.xml.replace("</ORDERS>", header + "</ORDERS>")));
        assertEquals(2, result.getOrderList().size());
        assertEquals(1, result.getEmptyHeaderCount());
    }

    // 영수증 구분자와 양수가 아닌 가격을 거절하는지 확인
    void testInvalidValues() {
        assertThrows(IllegalArgumentException.class, () -> orderXmlMapper.parse(bytes(OrderXmlData.xml.replace("홍길동", "홍^길동"))));
        assertThrows(IllegalArgumentException.class, () -> orderXmlMapper.parse(bytes(OrderXmlData.xml.replace("21000", "-1"))));
    }

    // DOCTYPE와 크기 초과 XML을 거절하는지 확인
    void testInvalidXml() {
        String xml = "<!DOCTYPE ORDERS [<!ENTITY x SYSTEM 'file:///must-not-be-read'>]><ORDERS>&x;</ORDERS>";
        assertThrows(IllegalArgumentException.class, () -> orderXmlMapper.parse(bytes(xml)));
        assertThrows(IllegalArgumentException.class, () -> orderXmlMapper.parse(new byte[1_048_577]));
    }

    // 예시인 알파벳 1 + 숫자 3인 4자리 ID가 중복 없이 이어지는지 확인
    void testIdGeneration() {
        var usedIds = new HashSet<String>();
        assertEquals("A000", IdGenerator.createId(usedIds));
        assertEquals("A001", IdGenerator.createId(usedIds));
        for (int index = 2; index < 1000; index++) IdGenerator.createId(usedIds);
        assertEquals("B000", IdGenerator.createId(usedIds));
    }

    public static void main(String[] args) throws Exception {
        {
            var test = new OrderXmlMapperTest();
            test.testHeaderItems();
        }
        {
            var test = new OrderXmlMapperTest();
            test.testEucKr();
            test.testXmlFragments();
        }
        {
            var test = new OrderXmlMapperTest();
            test.testPriceAndName();
        }
        {
            var test = new OrderXmlMapperTest();
            test.testNamespace();
        }
        {
            var test = new OrderXmlMapperTest();
            test.testMissingHeader();
        }
        {
            var test = new OrderXmlMapperTest();
            test.testDuplicateHeader();
        }
        {
            var test = new OrderXmlMapperTest();
            test.testHeaderWithoutItem();
        }
        {
            var test = new OrderXmlMapperTest();
            test.testInvalidValues();
        }
        {
            var test = new OrderXmlMapperTest();
            test.testInvalidXml();
        }
        {
            var test = new OrderXmlMapperTest();
            test.testIdGeneration();
        }
        System.out.println("확인 완료");
    }
}