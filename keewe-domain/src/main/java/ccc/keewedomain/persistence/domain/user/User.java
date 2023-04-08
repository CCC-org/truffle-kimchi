package ccc.keewedomain.persistence.domain.user;

import ccc.keewecore.consts.KeeweRtnConsts;
import ccc.keewecore.exception.KeeweException;
import ccc.keewedomain.dto.user.UserSignUpDto;
import ccc.keewedomain.persistence.domain.common.BaseTimeEntity;
import ccc.keewedomain.persistence.domain.common.Interest;
import ccc.keewedomain.persistence.domain.title.Title;
import ccc.keewedomain.persistence.domain.user.enums.Privacy;
import ccc.keewedomain.persistence.domain.user.enums.UserStatus;
import ccc.keewedomain.persistence.domain.user.enums.VendorType;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static javax.persistence.FetchType.LAZY;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(name = "user")
@Getter
@NoArgsConstructor(access = PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(name = "vendor_id")
    private String vendorId;

    @Column(name = "vendor_type")
    @Enumerated(value = EnumType.STRING)
    private VendorType vendorType;

    @Column(name = "email")
    private String email; // 아이디

    @Column(name = "password")
    private String password; // 비밀번호

    @Column(name = "phone_number", unique = true)
    private String phoneNumber; // 휴대전화 번호

    @Column(name = "nickname")
    private String nickname;

    @OneToOne(fetch = LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "profile_photo_id")
    private ProfilePhoto profilePhoto;

    @Column(name = "privacy")
    @Enumerated(EnumType.STRING)
    private Privacy privacy = Privacy.PUBLIC;

    @ElementCollection
    @CollectionTable(name = "favorite_interests", joinColumns = @JoinColumn(name = "user_id"))
    private List<Interest> interests = new ArrayList<>();

    //FIXME
//    @Column(name = "role")
//    @ElementCollection
//    @Enumerated(EnumType.STRING)
//    private List<UserRole> role; // ~~~/profile/{닉네임}


    @OneToMany(mappedBy = "follower", fetch = LAZY)
    private List<Follow> followers = new ArrayList<>(); // 내가 팔로우하는 사람들

    @OneToMany(mappedBy = "followee", fetch = LAZY)
    private List<Follow> followees = new ArrayList<>(); // 나를 팔로우하는 사람들

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "rep_title_id")
    private Title repTitle;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private UserStatus status = UserStatus.ONBOARD;

    @Column(name = "introduction")
    private String introduction = "";

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    public static User from(UserSignUpDto userSignUpDto) {
        User user = new User();
        user.vendorId = userSignUpDto.getVendorId();
        user.vendorType = userSignUpDto.getVendorType();
        user.email = userSignUpDto.getEmail();
        user.password = userSignUpDto.getPassword();
        user.phoneNumber = userSignUpDto.getPhoneNumber();

        return user;
    }

    public void initProfile(String nickname, Set<String> interests) {
        verifyOnboardStatus();
        this.nickname = nickname;
        this.interests = interests.stream().map(Interest::of).collect(Collectors.toList());
        this.status = UserStatus.ACTIVE;
    }

    public void verifyOnboardStatus() {
        if (this.status != UserStatus.ONBOARD) {
            throw new KeeweException(KeeweRtnConsts.ERR427);
        }
    }

    public void setProfilePhoto(ProfilePhoto profilePhoto) {
        this.profilePhoto = profilePhoto;
    }

    public void updateProfile(String nickname, Set<String> interests, Title repTitle, String introduction) {
        this.nickname = nickname;
        this.interests = interests.stream().map(Interest::of).collect(Collectors.toList());
        this.repTitle = repTitle;
        this.introduction = introduction;
    }

    public void deleteProfilePhoto() {
        if(profilePhoto != null) {
            this.profilePhoto.delete();
            this.profilePhoto = null;
        }
    }

    public String getProfilePhotoURL() {
        if (this.profilePhoto != null) {
            return profilePhoto.getImage();
        }
        return "";
    }

    public String getRepTitleName() {
        return repTitle == null ? "" : repTitle.getName();
    }
    public Long getRepTitleId() {
        return repTitle == null ? null : repTitle.getId();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return id.equals(user.id);
    }
}

