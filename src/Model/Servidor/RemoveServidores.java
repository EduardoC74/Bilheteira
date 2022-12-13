package Model.Servidor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

public class RemoveServidores extends Thread {
    ArrayList<Informacoes> listaServidores;
    AtomicBoolean threadCorre;

    public RemoveServidores( ArrayList<Informacoes> listaServidores,AtomicBoolean threadCorre) {
        this.listaServidores = listaServidores;
        this.threadCorre = threadCorre;
    }

    @Override
    public void run() {

        while (threadCorre.get()) {

            LocalDateTime now = LocalDateTime.now();

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

            synchronized (listaServidores) {
                Iterator<Informacoes> it = listaServidores.iterator();

                while (it.hasNext()) {
                    Informacoes info = it.next();
                    LocalDateTime servidor = LocalDateTime.parse(info.getCurrentTime(), formatter);

                    long seconds = ChronoUnit.SECONDS.between(servidor, now);
                    if(seconds > 35){
                        System.out.println("Servidor Desconectou-se [" + info.getPorto() + "]");
                        it.remove();
                        Servidor.atualiza("Desconectou-se",-4, null); // emite heartbeat quando se desconecta Servidor
                    }
                }
            }

        }
        System.out.println("[INFO] RemoveServidores terminado com sucesso!");
    }
}
