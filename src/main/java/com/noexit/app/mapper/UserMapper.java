package com.noexit.app.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.noexit.app.model.User;

@Mapper
public interface UserMapper {

	public int insertAccount(User user);
	public int insertInfo(User user);
	public int countByLoginId(String loginId);
	public int countByEmail(String email);
	public User selectByLoginId(User user);
	public User findByLoginId(String loginId);
	public int countCafeByUserId(Long userId);
	public User findByNameAndEmail(User user);

	// 비밀번호 찾기 아이디 + 이름으로 사용자 검증
	public User findByLoginIdAndName(User user);
	public int updatePassword(User user);

	// 회원탈퇴
	public int insertUserDrop(Long userId);
	public int deleteUserInfo(Long userId);
	// userId 로 저장된 비번 해시만 조회 (BCrypt matches 검증용)
	public String selectPasswordByUserId(Long userId);

}
