package Model.Servidor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class HeartBeat extends Thread  {
    private String dbName;
    private AtomicInteger versaoDB;
    private int portTCP;
    private InetAddress ipgroup;
    private int portServers;
    private MulticastSocket ms;
    private String ipServer;
    private AtomicInteger ligacoesTCP;
    private AtomicBoolean disponivel;
    private AtomicBoolean threadCorre;

    public HeartBeat(int portTCP, InetAddress ipgroup, int portServers, MulticastSocket ms,
                     String ipServer, AtomicInteger ligacoesTCP, AtomicInteger versao, String dbName, AtomicBoolean disponivel, AtomicBoolean threadCorre){
        this.portTCP = portTCP;
        this.ipgroup = ipgroup;
        this.portServers = portServers;
        this.ms = ms;
        this.ipServer = ipServer;
        this.ligacoesTCP = ligacoesTCP;
        this.versaoDB = versao;
        this.dbName = dbName;
        this.disponivel = disponivel;
        this.threadCorre = threadCorre;
    }

    @Override
    public void run() {
        System.out.println("Bem-vindo ao Servidor!["+portTCP+"]");
        while (threadCorre.get()) {
            try {

                LocalDateTime now = LocalDateTime.now();
                DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
                String currentTime = now.format(dateTimeFormatter);

                Informacoes info = new Informacoes(portTCP, ipServer, ligacoesTCP.get(),currentTime);
                info.setVersaoBd(versaoDB.get());
                info.setDbName(dbName);
                info.setDisponivel(disponivel.get());
                // usamos Atomic Integer pois é independente de sincronização (ThreadSafe) e é usado para contadores partilhados
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeUnshared(info);

                byte[] myMessageBytes = baos.toByteArray();

                DatagramPacket dp = new DatagramPacket(
                        myMessageBytes, myMessageBytes.length,
                        ipgroup, portServers
                );
                ms.send(dp);


                sleep(10000);
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        System.out.println("[INFO] HeartBeat terminado com sucesso!");
    }
}
