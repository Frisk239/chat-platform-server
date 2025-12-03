package com.chatplatform.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户登录请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLoginRequest {

    /**
     * 用户名或QQ号
     */
    @NotBlank(message = "用户名或QQ号不能为空")
    @Size(max = 50, message = "用户名或QQ号长度不能超过50个字符")
    private String usernameOrQqNumber;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 128, message = "密码长度必须在6-128个字符之间")
    private String password;

    /**
     * 是否记住我
     */
    private Boolean rememberMe = false;
}