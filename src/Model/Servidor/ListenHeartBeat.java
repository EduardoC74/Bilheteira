package Model.Servidor;

import utils.Msg;

import java.io.*;
import java.net.*;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static Model.Servidor.MensagensRetorno.CLIENTE_REGISTADO_SUCESSO;
import static Model.Servidor.Servidor.connDB;
import static Model.Servidor.Servidor.portServer;

public class ListenHeartBeat extends Thread{
    private MulticastSocket ms;
    ArrayList<Informacoes> listaServidores;
    static AtomicBoolean threadCorre;
    ArrayList<ObjectOutputStream> listOos;
    public ListenHeartBeat(MulticastSocket ms, ArrayList<Informacoes> listaServidores,AtomicBoolean threadCorre,ArrayList<ObjectOutputStream> listOos) {
        this.ms = ms;
        this.listaServidores = listaServidores;
        this.threadCorre = threadCorre;
        this.listOos = listOos;
    }

    @Override
    public void run() {
        while(threadCorre.get()) {
            DatagramPacket dp = new DatagramPacket(new byte[4000], 4000);
            try {
                ms.receive(dp);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            ByteArrayInputStream bais = new ByteArrayInputStream(dp.getData());
            ObjectInputStream ois;
            try {
                ois = new ObjectInputStream(bais);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            //Msg msg;
            Informacoes info;

            try {
                info = (Informacoes) ois.readObject();
                synchronized (listaServidores) {
                    //Informacoes info = new Informacoes(msg.getPortoServer(),msg.getIp(),msg.getLigacoesTCP());
                    if (!listaServidores.contains(info)){
                        /*&& info.isDisponivel()*/
                        listaServidores.add(info);
                        Comparator<Informacoes> compare = new InformacoesComparator();
                        listaServidores.sort(compare);

                        for (ObjectOutputStream os: listOos) {
                            enviaListaServidoresAtualizada(os);
                        }

                    }else { // se já existir

                        listaServidores.set(listaServidores.lastIndexOf(info), info);

                        Comparator<Informacoes> compare = new InformacoesComparator();
                        listaServidores.sort(compare);
                    }


                }

                if (info.getMsgAtualiza() != null) {

                    ArrayList<String> msgSocket;

                    if (info.getMsgAtualiza().equalsIgnoreCase("PREPARE") && info.getPorto() != portServer) {

                        msgSocket = info.getMsgSockett();

                        enviaUDP(info.getPortoUDPAtualiza(), info.getVersaoBdAtualiza(), info.getIp());
                            LocalDateTime entraWhile = LocalDateTime.now();
                        while (true) {
                            LocalDateTime atual = LocalDateTime.now();
                            long seconds = ChronoUnit.SECONDS.between(entraWhile,atual);

                            if(seconds > 2){
                                System.out.println("Timeout de multicast!");
                                break;
                            }

                            ms.receive(dp);
                            bais = new ByteArrayInputStream(dp.getData());
                            try {
                                ois = new ObjectInputStream(bais);
                                info = (Informacoes) ois.readObject();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            if (info.getMsgAtualiza() != null) {
                                if (info.getMsgAtualiza().equalsIgnoreCase("Commit")) {
                                    if(msgSocket!=null){
                                        processaAtualizacao(msgSocket);
                                    }
                                    break;
                                } else if (info.getMsgAtualiza().equalsIgnoreCase("Abort")) {
                                    System.out.println("Recebi Abort");
                                    break;
                                }
                            }
                        }
                    }
                }
            } catch (IOException | ClassNotFoundException | SQLException e) {
                throw new RuntimeException(e);
            }
        }
        System.out.println("[INFO] ListenHeartBeat terminado com sucesso!");
    }

    private void processaAtualizacao(ArrayList<String> msgSocket) throws SQLException, IOException {
        System.out.println("\nA atualizar base de dados...");
        Msg msg = new Msg();
        switch (msgSocket.get(0)) {
            case "REGISTA_USER" -> {
                if (connDB.insertUser(msgSocket, false) == CLIENTE_REGISTADO_SUCESSO) {
                    msg.setMsg("\nA base de dados foi atualizada! (Um usuário registou uma conta)");
                }
            }
            case "LOGIN_USER" -> {
                connDB.logaUser(msgSocket, false);
                msg.setMsg("\nA base de dados foi atualizada! (Login de um usuário)");
            }
            case "EDITA_NAME" -> {
                connDB.updateUser(msgSocket, 0,false);
                msg.setMsg("\nA base de dados foi atualizada! (Um usuário editou o seu nome)");
            }
            case "EDITA_USERNAME" -> {
                connDB.updateUser(msgSocket, 1,false);
                msg.setMsg("\nA base de dados foi atualizada! (Um usuário editou o seu username)");
            }
            case "EDITA_PASSWORD" -> {
                connDB.updateUser(msgSocket, 2,false);
                msg.setMsg("\nA base de dados foi atualizada! (Um usuário editou a sua password)");
            }
            case "INSERE_ESPETACULOS" -> {
                connDB.insereEspetaculos(msgSocket,false);
                msg.setMsg("\nA base de dados foi atualizada! (Admin inseriu um espetáculo)");
            }
            case "TORNA_VISIVEL" -> {
                connDB.tornaVisivel(msgSocket,false);
                msg.setMsg("\nA base de dados foi atualizada! (Admin tornou um espetáculo visivel)");
            }
            case "SUBMETE_RESERVA"->{
                connDB.submeteReserva(msgSocket,false);
                msg.setMsg("\nA base de dados foi atualizada! (Um usuário submeteu uma reserva)");
            }
            case "EFETUA_PAGAMENTO"->{
                connDB.efetuaPagamento(msgSocket,false);
                msg.setMsg("\nA base de dados foi atualizada! (Um usuário efetuou um pagamento)");
            }
            case "LIMITE_TEMPO"->{
                connDB.retiraReservaLimiteTempo(msgSocket,false);
                msg.setMsg("\nA base de dados foi atualizada! (Reserva de um usuário eliminada devido ao limite de tempo)");
            }
            case "ELIMINA_ESPETACULO"->{
                connDB.eliminarEspetaculo(msgSocket,false);
                msg.setMsg("\nA base de dados foi atualizada! (Admin eliminou um espetáculo)");
            }
            case "LOGOUT"->{
                connDB.logout(msgSocket,false);
                msg.setMsg("\nA base de dados foi atualizada! (Um usuário encerrou a sessão)");
            }

        }
        for (ObjectOutputStream os: listOos) {
            os.writeUnshared(msg);
        }
    }

    private void enviaUDP(int portoUDPAtualiza, int versaoBdAtualizada, String ip) throws IOException {
        DatagramSocket ds = new DatagramSocket();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        Msg msg = new Msg();
        msg.setVersaoBdAtualizada(versaoBdAtualizada);

        oos.writeUnshared(msg);
        byte[] messageBytes = baos.toByteArray();

        InetAddress ipServer = InetAddress.getByName(ip);

        DatagramPacket dp = new DatagramPacket(messageBytes, messageBytes.length, ipServer, portoUDPAtualiza);
        ds.send(dp);
    }

    void enviaListaServidoresAtualizada(ObjectOutputStream oos) {
        Msg msg = new Msg();
        try {
            Iterator<Informacoes> iterator = listaServidores.iterator();

            while (iterator.hasNext()) {
                Informacoes info = iterator.next();

                msg.setPortoServer(info.getPorto());
                msg.setIp(info.getIp());
                msg.setLigacoesTCP(info.getLigacoes());
                msg.setIndex(listaServidores.indexOf(info));

                if (!iterator.hasNext()) {
                    msg.setLastPacket(true);
                }
                oos.writeUnshared(msg);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}



