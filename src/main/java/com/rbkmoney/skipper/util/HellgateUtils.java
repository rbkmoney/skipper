package com.rbkmoney.skipper.util;

import com.rbkmoney.damsel.payment_processing.ServiceUser;
import com.rbkmoney.damsel.payment_processing.UserInfo;
import com.rbkmoney.damsel.payment_processing.UserType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class HellgateUtils {

    public static final String USER_INFO_ID = "skipper";

    public static final UserInfo USER_INFO = new UserInfo()
            .setId(USER_INFO_ID)
            .setType(UserType.service_user(new ServiceUser()));

}
