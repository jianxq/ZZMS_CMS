/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc> 
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License version 3
 as published by the Free Software Foundation. You may not use, modify
 or distribute this program under any other version of the
 GNU Affero General Public License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package handling.login;

import client.MapleClient;
import constants.ServerConstants;
import handling.channel.ChannelServer;
import handling.login.handler.CharLoginHandler;
import java.util.Map;
import java.util.Map.Entry;
import server.Timer.PingTimer;
import tools.packet.CWvsContext;
import tools.packet.LoginPacket;

public class LoginWorker {

    private static long lastUpdate = 0;

    public static void registerClient(final MapleClient c) {
        if (LoginServer.isAdminOnly() && !c.isGM()) {
            c.getSession().write(CWvsContext.broadcastMsg(1, "服务器维护中....\r\n现在只允许维护人员进入.\r\n请稍后再试."));
            c.getSession().write(LoginPacket.getLoginFailed(7));
            return;
        }

        if (System.currentTimeMillis() - lastUpdate > 600000) { // Update once every 10 minutes
            lastUpdate = System.currentTimeMillis();
            final Map<Integer, Integer> load = ChannelServer.getChannelLoad();
            int usersOn = 0;
            if (load == null || load.size() <= 0) { // In an unfortunate event that client logged in before load
                lastUpdate = 0;
                c.getSession().write(LoginPacket.getLoginFailed(7));
                return;
            }
            final double loadFactor = 1200 / ((double) LoginServer.getUserLimit() / load.size());
            for (Entry<Integer, Integer> entry : load.entrySet()) {
                usersOn += entry.getValue();
                load.put(entry.getKey(), Math.min(1200, (int) (entry.getValue() * loadFactor)));
            }
            LoginServer.setLoad(load, usersOn);
            lastUpdate = System.currentTimeMillis();
        }

        if (c.finishLogin() == 0) {
            c.getSession().write(LoginPacket.getAuthSuccessRequest(c));
            if (ServerConstants.MAPLE_TYPE == ServerConstants.MapleType.中国) {
                
                CharLoginHandler.ServerListRequest(c);
            }

            c.setIdleTask(PingTimer.getInstance().schedule(new Runnable() {

                @Override
                public void run() {
                    c.getSession().close(true);
                }
            }, 10 * 60 * 10000));
        } else {
            c.getSession().write(LoginPacket.getLoginFailed(7));
        }
    }
}
