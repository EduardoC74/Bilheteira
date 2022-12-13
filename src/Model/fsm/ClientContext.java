package Model.fsm;

import Model.Cliente.ClientData;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientContext {
    private IClientState state;
    ClientData data;

    public ClientContext(String strIpServer, int portServer) {
        data = new ClientData(strIpServer,portServer);
        state = ClientState.AUTENTICA.createState(this, data);
    }
    public void estadoSeguinte(IClientState newState) {
        state = newState;
    }
    public ClientState getState() {
        if (state == null) return null;
        return state.getState();
    }
    public void avancar(int i) {
        state.avancar(i);
    }
    public void conectaTCP(AtomicInteger confirma){
        data.connectaTCPServidor(confirma);
    }

    public boolean regista(ArrayList<String> temp) {
        return state.regista(temp);
    }

    public boolean login(ArrayList<String> temp) {
        return state.login(temp);
    }

    public boolean conectaUDP() {
        return  data.connectaServidorUDP();
    }

    public boolean edita(ArrayList<String> temp) { return state.edita(temp);}

    public boolean inserirEspetaculos(ArrayList<String> temp) {
        return state.inserirEspetaculo(temp);
    }

    public boolean visivel(ArrayList<String> temp) {
        return state.visivel(temp);
    }

    public boolean consulta(ArrayList<String> temp) {
        return state.consulta(temp);
    }

    public boolean selecionarEspetaculo(ArrayList<String> temp) { return state.consulta(temp); }

    public boolean submeteReserva(ArrayList<String> temp) { return state.submeteReserva(temp); }

    public boolean esperaPagamento(AtomicInteger pagamento) {
        return state.esperaPagamento(pagamento);
    }

    public boolean efetuaPagamento(ArrayList<String> temp) {
        return state.efetuaPagamento(temp);
    }

    public boolean limiteTempo(ArrayList<String> temp) {
        return state.limiteTempo(temp);
    }
    public boolean consultaReservasPagas(ArrayList<String> temp) {
        return state.consultaReservasPagas(temp);
    }
    public boolean consultaReservasPendentes(ArrayList<String> temp) {
        return state.consultaReservasPendentes(temp);
    }
    public void regressar() {
        state.regressar();
    }

    public boolean eliminarEspetaculo(ArrayList<String> temp) {
        return state.eliminarEspetaculo(temp);
    }

    public boolean logout(ArrayList<String> temp) {
        return state.logout(temp);
    }
}
