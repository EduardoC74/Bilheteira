package Model.fsm;

import Model.Cliente.ClientData;

import java.util.ArrayList;

public class LogadoAdminState extends ClientAdapter {
    public LogadoAdminState(ClientContext context, ClientData data) {
        super(context,data);
    }

    @Override
    public boolean inserirEspetaculo(ArrayList<String> temp) {
        return data.enviaInfo(temp);
    }

    @Override
    public boolean visivel(ArrayList<String> temp) {
        return data.enviaInfo(temp);
    }

    @Override
    public boolean consulta(ArrayList<String> temp) {
        return data.enviaInfo(temp);
    }

    @Override
    public boolean eliminarEspetaculo(ArrayList<String> temp) {return data.enviaInfo(temp);}
    @Override
    public boolean logout(ArrayList<String> temp) {
        return data.enviaInfo(temp);
    }
    @Override
    public void regressar() {
        estadoSeguinte(ClientState.AUTENTICA);
    }
    @Override
    public ClientState getState() {
        return ClientState.LOGADO_ADMIN;
    }
}
