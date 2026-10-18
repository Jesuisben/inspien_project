// 주문과 운송에 쓰이는 ID 생성하는 파일

package com.inspien_project.common;

import java.util.Locale;
import java.util.Set;

public final class IdGenerator {

    private IdGenerator() { }

    // 사용 중인 ID 집합에 새 ID도 즉시 선점해서 같은 요청 안에 ID 중복 발급 방지
    public static String createId(Set<String> usedIds) {
        for (int number = 0; number < 26_000; number++) {
            String id = String.format(Locale.ROOT, "%c%03d", 'A' + number / 1000, number % 1000);
            if (usedIds.add(id)) return id;
        }
        throw new IllegalStateException("이 지원자 키에 사용 가능 한 ID가 없음");
    }
}