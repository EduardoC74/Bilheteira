package Model.Cliente;

import Model.fsm.ClientContext;
import Ui.ClienteUI;

public class Cliente {
    static String strIpServer;
    static int portServer;


    public static void main(String[] args) throws Exception {
        if (args.length != 2)
            return;
        strIpServer = args[0];
        portServer = Integer.parseInt(args[1]);

        ClientContext fsm = new ClientContext(strIpServer, portServer);
        ClienteUI ui = new ClienteUI(fsm);
        ui.start();

    }


}
