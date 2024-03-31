package com.aizistral.infmachine.utils;

import com.aizistral.infmachine.InfiniteMachine;
import lombok.experimental.UtilityClass;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

import java.util.List;

@UtilityClass
public class Utils {
    public static final long DELETED_USER_ID = 456226577798135808L;

    public boolean hasRole(Member member, Long roleID) {
        List<Role> rolesOfAuthor = member.getRoles();
        for(Role role : rolesOfAuthor) {
            if(role.getIdLong() == roleID) return true;
        }
        return false;
    }

    public Role getRoleByID(long roleID) {
        List<Role> domainRoles = InfiniteMachine.INSTANCE.getDomain().getRoles();
        for(Role role : domainRoles) {
            if(role.getIdLong() == roleID) return role;
        }
        return null;
    }
}
