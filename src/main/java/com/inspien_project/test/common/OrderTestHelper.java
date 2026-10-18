// 검증 공통 파일

package com.inspien_project.test.common;

public final class OrderTestHelper {

    private OrderTestHelper() { }

    // 검증 파일의 재사용 메소드
    public static void assertEquals(Object expected, Object actual) {
        if (!java.util.Objects.equals(expected, actual)) throw new IllegalStateException("기대값과 실제값 불일치");
    }

    public static void assertTrue(boolean value) {
        if (!value) throw new IllegalStateException("검증 조건 불일치");
    }

    public static void assertArrayEquals(byte[] expected, byte[] actual) {
        assertTrue(java.util.Arrays.equals(expected, actual));
    }

    public static void assertNotNull(Object value) { assertTrue(value != null); }

    // 예외처리 검증 중첩 인터페이스임
    public interface CheckAction { void run() throws Exception; }

    public static void assertThrows(Class<? extends Throwable> expected, CheckAction action) {
        try { action.run(); }
        catch (Throwable error) {
            if (expected.isInstance(error)) return;
            throw new IllegalStateException("예상하지 않은 예외 종류", error);
        }
        throw new IllegalStateException("예상한 예외가 발생하지 않음");
    }
}