package com.aizistral.infmachine.utils;

import java.util.List;

import lombok.experimental.UtilityClass;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

@UtilityClass
public class Utils {
    private static final SimpleLogger LOGGER = new SimpleLogger("Utilities");
    public static final long DELETED_USER_ID = 456226577798135808L;

    public boolean hasRole(Member member, Long roleID) {
        List<Role> rolesOfAuthor = member.getRoles();
        for(Role role : rolesOfAuthor) {
            if(role.getIdLong() == roleID) return true;
        }
        return false;
    }

}
