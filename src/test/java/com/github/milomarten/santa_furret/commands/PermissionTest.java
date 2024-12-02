package com.github.milomarten.santa_furret.commands;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PermissionTest {
    @Test
    public void sanityTest() {
        String s = Permission.group(Permission.MANAGE_GUILD, Permission.SEND_MESSAGES);

        assertEquals("2080", s);
    }

    @Test
    public void sanityTest2() {
        String s = Permission.MANAGE_GUILD.single();

        assertEquals("32", s);
    }
}