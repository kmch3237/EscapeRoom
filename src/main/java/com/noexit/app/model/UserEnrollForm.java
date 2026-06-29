package com.noexit.app.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 회원가입 전용 폼 DTO (도메인 User 와 분리).
 *  - 검증 규칙(@NotBlank 등)을 여기 모아둠 → 공유 User 가 검증에 오염되지 않음.
 *  - 검증 통과 후 toUser() 로 도메인 객체(User)로 변환해 service 에 넘김.
 *  - 규칙은 기존 JS 검증과 동일하게 맞춤(서버에서 한 번 더 = 진짜 방어선).
 */
@Getter
@Setter
public class UserEnrollForm {

    @NotBlank(message = "아이디는 필수입니다.")
    @Pattern(regexp = "^(?=.*[a-zA-Z])[a-zA-Z0-9]{6,15}$",
             message = "아이디는 6~15자 영문+숫자(영문 1자 이상)여야 합니다.")
    private String loginId;

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
    private String password;

    @NotBlank(message = "닉네임은 필수입니다.")
    @Size(min = 2, max = 10, message = "닉네임은 2~10자여야 합니다.")
    private String nickname;

    @NotBlank(message = "이름은 필수입니다.")
    private String name;

    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    @NotBlank(message = "연락처는 필수입니다.")
    @Pattern(regexp = "\\d{11}", message = "연락처는 숫자 11자리여야 합니다.")
    private String phone;

    @NotBlank(message = "성별을 선택해주세요.")
    private String gender;

    @NotBlank(message = "생년월일을 선택해주세요.")
    private String birthDate;

    /** 검증 통과 후 도메인 User 로 변환 */
    public User toUser() {
        User user = new User();
        user.setLoginId(loginId);
        user.setPassword(password);
        user.setNickname(nickname);
        user.setName(name);
        user.setEmail(email);
        user.setPhone(phone);
        user.setGender(gender);
        user.setBirthDate(birthDate);
        return user;
    }
}
