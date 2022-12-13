package Model.fsm;

import Model.Cliente.ClientData;

public enum ClientState {
    AUTENTICA, LOGADO_ADMIN, LOGADO_USER,PAGAMENTO;

    IClientState createState(ClientContext context, ClientData data) {
        return switch (this) {
            case AUTENTICA -> new AutenticaState(context, data);
            case LOGADO_ADMIN -> new LogadoAdminState(context,data);
            case LOGADO_USER -> new LogadoUserState(context,data);
            case PAGAMENTO -> new PagamentoUserState(context,data);
        };
    }
}
