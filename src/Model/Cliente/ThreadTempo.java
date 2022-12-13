package Model.Cliente;

import utils.Pagamento;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadTempo extends Thread {
    AtomicInteger pagamento;
    Scanner sc;
    public ThreadTempo(AtomicInteger pagamento) {
        this.pagamento = pagamento;
    }

    @Override
    public void run() {
        Thread scan = new ThreadScan(pagamento);
        scan.start();
        LocalDateTime now = LocalDateTime.now();

        while (pagamento.get() != Pagamento.LIMITE_TEMPO.ordinal() && pagamento.get()!= Pagamento.EFETUA_PAGAMENTO.ordinal()) {
            LocalDateTime atualizado = LocalDateTime.now();
            long seconds = ChronoUnit.SECONDS.between(now, atualizado);

            if (seconds > 10) {
                pagamento.getAndSet( Pagamento.LIMITE_TEMPO.ordinal());
                break;
            }
        }
        scan.interrupt();
    }
}


