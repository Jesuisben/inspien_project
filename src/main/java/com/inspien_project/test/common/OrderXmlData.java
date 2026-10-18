
package com.inspien_project.test.common;

public final class OrderXmlData {

    private OrderXmlData() { }

    public static final String xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <ORDERS>
              <HEADER><USER_ID>USER1</USER_ID><NAME>홍길동</NAME><ADDRESS>서울 금천구</ADDRESS><STATUS>N</STATUS></HEADER>
              <ITEM><USER_ID>USER1</USER_ID><ITEM_ID>ITEM1</ITEM_ID><ITEM_NAME>청바지</ITEM_NAME><PRICE>21000</PRICE></ITEM>
              <ITEM><USER_ID>USER1</USER_ID><ITEM_ID>ITEM2</ITEM_ID><ITEM_NAME>티셔츠</ITEM_NAME><PRICE>15800</PRICE></ITEM>
            </ORDERS>
            """;
}