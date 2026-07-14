package org.afo.llm.domain.vo;

import org.afo.common.translation.annotation.Translation;
import org.afo.common.translation.constant.TransConstant;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("dev")
class LlmApiKeyVoTest {

    @Test
    void ownerUserNameTranslatesFromOwnerUserId() throws NoSuchFieldException {
        Translation translation = LlmApiKeyVo.class
            .getDeclaredField("ownerUserName")
            .getAnnotation(Translation.class);

        assertEquals(TransConstant.USER_ID_TO_NAME, translation.type());
        assertEquals("ownerUserId", translation.mapper());
    }
}
