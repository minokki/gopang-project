package com.gopang.service;

import com.gopang.account.UserAccount;
import com.gopang.constant.Role;
import com.gopang.dto.MemberSearchDto;
import com.gopang.dto.SignUpForm;
import com.gopang.entity.Account;
import com.gopang.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AccountService implements UserDetailsService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final TemplateEngine templateEngine;


    /* 회원가입 토큰생성 */
    public Account processNewAccount(SignUpForm signUpForm) {
        Account newAccount = saveNewAccount(signUpForm); //아래 메서드는 영속성 컨텍스트 상태임. 그 정보를 가지고 토큰 생성후 저장하려면, 트랜젝션 어노테이션 작성해줘야함
        //이메일 체크토큰 생성
        newAccount.generateEmailCheckToken();
//        sentConfirmEmail(newAccount);
        return newAccount;
    }

    /* 회원가입 */
    public Account saveNewAccount(SignUpForm signUpForm) {
        Account account = Account.builder()
                .email(signUpForm.getEmail())
                .nickname(signUpForm.getNickname())
                .password(passwordEncoder.encode(signUpForm.getPassword())) //패스워드 인코드
                .role(Role.USER)
                .userType(signUpForm.getUserType())
                .build();
        Account newAccount = accountRepository.save(account);  //회원저장
        return newAccount;
    }

    /* 로그인 */
    public void login(Account account) {
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                new UserAccount(account), //사용자 정보
                account.getPassword(), //사용자 패스워드
                new UserAccount(account).getAuthorities()); // 여기서 authorities 메서드를 이용하여 권한 추가
        SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(token);
    }

    /* ??? */
    @Transactional(readOnly = true)
    @Override
    public UserDetails loadUserByUsername(String emailOrNickname) throws UsernameNotFoundException {
        Account account = accountRepository.findByEmail(emailOrNickname);
        if(account == null){
            account = accountRepository.findByNickname(emailOrNickname);
        }

        if(account == null){
            throw new UsernameNotFoundException(emailOrNickname);
        }

        return new UserAccount(account);
    }

    /* 회원가입시 자동로그인 */
    public void completeSingUP(Account account) {
        account.completeSignUp();
        login(account);
    }

    /* 프로필 UPDATE */
//    public void updateProfile(Account account, Profile profile) {
//        account.setUrl(profile.getUrl());
//        account.setOccupation(profile.getOccupation());
//        account.setLocation(profile.getLocation());
//        account.setBio(profile.getBio());
//        account.setProfileImage(profile.getProfileImage());
//        accountRepository.save(account);
//    }

    /* 프로필 PASSWORD */
    public void updatePassword(Account account, String newPassword) {
        account.setPassword(passwordEncoder.encode(newPassword));
        accountRepository.save(account);
    }
    /* 프로필 NICKNAME */
    public void updateNickname(Account account, String nickname) {
        account.setNickname(nickname);
        accountRepository.save(account);
        login(account);
    }

    public Page<Account> getAdminMemberPage(MemberSearchDto memberSearchDto, Pageable pageable) {
        return accountRepository.getAdminMemberPage(memberSearchDto, pageable);
    }
}
