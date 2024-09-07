package com.aizistral.infmachine.utils;

import lombok.experimental.UtilityClass;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;

import java.util.Arrays;
import java.util.List;

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
