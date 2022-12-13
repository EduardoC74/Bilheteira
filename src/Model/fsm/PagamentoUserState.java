package Model.fsm;

import Model.Cliente.ClientData;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class PagamentoUserState extends ClientAdapter {
    public PagamentoUserState(ClientContext context, ClientData data) {
        super(context,data);
    }

    @Override
    public boolean consultaReservasPagas(ArrayList<String> temp) {
        return data.enviaInfo(temp);
    }

    @Override
    public boolean consultaReservasPendentes(ArrayList<String> temp) {
        return data.enviaInfo(temp);
    }

    @Override
    public boolean esperaPagamento(AtomicInteger pagamento) {
        return data.esperaPagamento(pagamento);
    }

    @Override
    public boolean efetuaPagamento(ArrayList<String> temp) {
        return data.enviaInfo(temp);
    }

    @Override
    public boolean limiteTempo(ArrayList<String> temp) {
        return data.enviaInfo(temp);
    }

    @Override
    public void regressar() {
        estadoSeguinte(ClientState.LOGADO_USER);
    }

    @Override
    public ClientState getState() {
        return ClientState.PAGAMENTO;
    }

}
