package ccc.keeweapi.dto.user;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(staticName = "of")
public class UnblockUserResponse {
    private Long unblockedUserId;
}
