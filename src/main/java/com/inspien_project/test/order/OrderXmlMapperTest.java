
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

    void testHeaderItems() {
        var orderList = orderXmlMapper.parse(bytes(OrderXmlData.xml)).getOrderList();
        assertEquals(2, orderList.size());
        assertEquals("홍길동", orderList.get(1).getName());
        assertEquals("ITEM2", orderList.get(1).getItemId());
    }

    void testEucKr() {
        String xml = OrderXmlData.xml.replace("UTF-8", "EUC-KR");
        assertEquals("청바지", orderXmlMapper.parse(xml.getBytes(Charset.forName("EUC-KR"))).getOrderList().getFirst().getItemName());
    }

    void testXmlFragments() {
        String xml = OrderXmlData.xml.replaceFirst("<\\?xml[^?]*\\?>", "")
                .replace("<ORDERS>", "").replace("</ORDERS>", "");
        var result = orderXmlMapper.parse(xml.getBytes(Charset.forName("EUC-KR")));
        assertEquals(2, result.getOrderList().size());
        assertEquals("청바지", result.getOrderList().getFirst().getItemName());
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

    void testPriceAndName() {
        var orderList = orderXmlMapper.parse(bytes(OrderXmlData.xml.replace("21000", "021000").replace("홍길동", " 홍길동 "))).getOrderList();
        assertEquals("021000", orderList.getFirst().getPrice());
        assertEquals(" 홍길동 ", orderList.getFirst().getName());
    }

    void testNamespace() {
        String xml = OrderXmlData.xml.replace("<ORDERS>", "<ORDERS xmlns='urn:orders'>");
        assertEquals(2, orderXmlMapper.parse(bytes(xml)).getOrderList().size());
    }

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

    void testDuplicateHeader() {
        String header = "<HEADER><USER_ID>USER1</USER_ID><NAME>김</NAME><ADDRESS>서울</ADDRESS><STATUS>N</STATUS></HEADER>";
        assertThrows(IllegalArgumentException.class, () -> orderXmlMapper.parse(bytes(OrderXmlData.xml.replace("</ORDERS>", header + "</ORDERS>"))));
    }

    void testHeaderWithoutItem() {
        String header = "<HEADER><USER_ID>USER2</USER_ID><NAME>김</NAME><ADDRESS>서울</ADDRESS><STATUS>N</STATUS></HEADER>";
        var result = orderXmlMapper.parse(bytes(OrderXmlData.xml.replace("</ORDERS>", header + "</ORDERS>")));
        assertEquals(2, result.getOrderList().size());
        assertEquals(1, result.getEmptyHeaderCount());
    }

    void testInvalidValues() {
        assertThrows(IllegalArgumentException.class, () -> orderXmlMapper.parse(bytes(OrderXmlData.xml.replace("홍길동", "홍^길동"))));
        assertThrows(IllegalArgumentException.class, () -> orderXmlMapper.parse(bytes(OrderXmlData.xml.replace("21000", "-1"))));
    }

    void testInvalidXml() {
        String xml = "<!DOCTYPE ORDERS [<!ENTITY x SYSTEM 'file:///must-not-be-read'>]><ORDERS>&x;</ORDERS>";
        assertThrows(IllegalArgumentException.class, () -> orderXmlMapper.parse(bytes(xml)));
        assertThrows(IllegalArgumentException.class, () -> orderXmlMapper.parse(new byte[1_048_577]));
    }

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