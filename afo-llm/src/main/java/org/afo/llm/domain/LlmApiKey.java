package org.afo.llm.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.afo.common.tenant.core.TenantEntity;

import java.io.Serial;
import java.util.Date;

/**
 * LLM 业务 API Key 对象 afo_llm_api_key。
 *
 * @author afo
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("afo_llm_api_key")
public class LlmApiKey extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * API Key ID。
     */
    @TableId(value = "key_id")
    private Long keyId;

    /**
     * 应用客户端ID。
     */
    private Long clientId;

    /**
     * 所属用户ID。
     */
    private Long ownerUserId;

    /**
     * Key 名称。
     */
    private String keyName;

    /**
     * API Key 定位前缀，用于检索，不对外明文返回。
     */
    private String keyPrefix;

    /**
     * Key 哈希值，不存储明文。
     */
    private String keyHash;

    /**
     * 授权模型编码，多个用逗号分隔，* 表示全部模型。
     */
    private String keyScope;

    /**
     * 过期时间。
     */
    private Date expireTime;

    /**
     * 最后使用时间。
     */
    private Date lastUsedTime;

    /**
     * X-Team 标签。
     */
    @TableField(exist = false)
    private String teamTag;

    /**
     * 状态（0正常 1停用）。
     */
    private String status;

    /**
     * 删除标志（0代表存在 1代表删除）。
     */
    @TableLogic
    private String delFlag;

    /**
     * 备注。
     */
    private String remark;
}
