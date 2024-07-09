package com.aizistral.infmachine.utils;

import com.aizistral.infmachine.config.InfiniteConfig;
import lombok.experimental.UtilityClass;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;

import java.util.Arrays;
import java.util.List;

@UtilityClass
public class Utils {
    private static final StandardLogger LOGGER = new StandardLogger("Utilities");
    public static final long DELETED_USER_ID = 456226577798135808L;

    public boolean hasRole(Member member, Long roleID) {
        List<Role> rolesOfAuthor = member.getRoles();
        for(Role role : rolesOfAuthor) {
            if(role.getIdLong() == roleID) return true;
        }
        return false;
    }

    public Role getRoleByID(long roleID) {
        List<Role> domainRoles = InfiniteConfig.INSTANCE.getDomain().getRoles();
        for(Role role : domainRoles) {
            if(role.getIdLong() == roleID) return role;
        }
        return null;
    }

    //Quite slow so should only be called when absolutely necessary
    public static Member userToMember(User user) {
        if(user == null) return null;
        Member member = null;
        try {
            member = InfiniteConfig.INSTANCE.getDomain().retrieveMember(user).complete();
        } catch (ErrorResponseException e) {
            if(e.getErrorCode() == 10007) return null; //member left the domain
            else {
                LOGGER.error(e.getMessage());
                LOGGER.error(Arrays.toString(e.getStackTrace()));
            }
        }
        return member;
    }
}
