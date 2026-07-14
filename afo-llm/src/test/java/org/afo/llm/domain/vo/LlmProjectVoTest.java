package org.afo.llm.domain.vo;

import org.afo.common.translation.annotation.Translation;
import org.afo.common.translation.constant.TransConstant;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag("dev")
class LlmProjectVoTest {

    @Test
    void ownerUserNameTranslatesFromOwnerUserId() throws NoSuchFieldException {
        Translation translation = LlmProjectVo.class
            .getDeclaredField("ownerUserName")
            .getAnnotation(Translation.class);

        assertNotNull(translation);
        assertEquals(TransConstant.USER_ID_TO_NAME, translation.type());
        assertEquals("ownerUserId", translation.mapper());
    }
}
