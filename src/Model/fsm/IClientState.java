package Model.fsm;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public interface IClientState {
    ClientState getState();
    boolean regista(ArrayList<String> temp);
    boolean login(ArrayList<String> temp);
    boolean edita(ArrayList<String> temp);
    void avancar(int i);
    boolean inserirEspetaculo(ArrayList<String> temp);

    boolean visivel(ArrayList<String> temp);

    void regressar();

    boolean consulta(ArrayList<String> temp);
    boolean consultaReservasPagas(ArrayList<String> temp);
    boolean consultaReservasPendentes(ArrayList<String> temp);
    boolean esperaPagamento(AtomicInteger pagamento);

    boolean submeteReserva(ArrayList<String> temp);

    boolean efetuaPagamento(ArrayList<String> temp);

    boolean limiteTempo(ArrayList<String> temp);

    boolean eliminarEspetaculo(ArrayList<String> temp);

    boolean logout(ArrayList<String> temp);
}
