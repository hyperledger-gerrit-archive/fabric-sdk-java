package org.hyperledger.fabric.sdk.identity;

import org.hyperledger.fabric.protos.common.MspPrincipal;

public enum IdemixRoles {
    MEMBER(1),
    ADMIN(2),
    CLIENT(4),
    PEER(8);
    // Next roles values: 8, 16, 32 ..

    public final int value;

    IdemixRoles(int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

    public static int getRoleMask(IdemixRoles[] roles) {
        int mask = 0;
        for (IdemixRoles role: roles) {
            mask = mask | role.value;
        }
        return mask;
    }

    public static boolean checkRole(int role, IdemixRoles searchRole) {
        return (role & searchRole.value) == searchRole.value;
    }

    public static int getIdemixRoleFromMSPRole(MspPrincipal.MSPRole role) {
        switch (role.getRoleValue()) {
            case MspPrincipal.MSPRole.MSPRoleType.ADMIN_VALUE:
                return ADMIN.getValue();
            case MspPrincipal.MSPRole.MSPRoleType.MEMBER_VALUE:
                return MEMBER.getValue();
            case MspPrincipal.MSPRole.MSPRoleType.PEER_VALUE:
                return PEER.getValue();
            case MspPrincipal.MSPRole.MSPRoleType.CLIENT_VALUE:
                return CLIENT.getValue();
            default:
                return MEMBER.getValue();
        }
    }

    public static MspPrincipal.MSPRole.MSPRoleType getMSPRoleFromIdemixRole(int role) {
        if (role == ADMIN.getValue()) {
            return MspPrincipal.MSPRole.MSPRoleType.ADMIN;
        }

        if (role == CLIENT.getValue()) {
            return MspPrincipal.MSPRole.MSPRoleType.CLIENT;
        }

        if (role == PEER.getValue()) {
            return MspPrincipal.MSPRole.MSPRoleType.PEER;
        }

        return MspPrincipal.MSPRole.MSPRoleType.MEMBER;
    }
}