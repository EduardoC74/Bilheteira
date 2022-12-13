package Model.fsm;

import Model.Cliente.ClientData;

import java.util.ArrayList;


public class AutenticaState extends ClientAdapter {
    public AutenticaState(ClientContext context, ClientData data) {
        super(context,data);
    }

    @Override
    public boolean regista(ArrayList<String> temp) {
        return data.enviaInfo(temp);
    }

    @Override
    public boolean login(ArrayList<String> temp) {
        return data.enviaInfo(temp);
    }

    @Override
    public void avancar(int i) {
        if(i == 1)
            estadoSeguinte(ClientState.LOGADO_USER);
        else if(i == 2)
            estadoSeguinte(ClientState.LOGADO_ADMIN);
    }

    @Override
    public ClientState getState() {
        return ClientState.AUTENTICA;
    }
}
