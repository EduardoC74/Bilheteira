package Model.Servidor;

import utils.Msg;

import java.io.*;
import java.net.*;
import java.sql.SQLException;
import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static Model.Servidor.Servidor.connDB;

public class ComunicaTCP extends Thread {
    Socket socketCli;
    AtomicInteger ligacoesTCP;
    String dbName;
    ArrayList<ObjectOutputStream> listaOos;
    AtomicBoolean disponivel;
    AtomicBoolean threadCorre;
    ArrayList<Informacoes> listaServidores;

    public ComunicaTCP(Socket socketCli, AtomicInteger ligacoesTCP, String dbName, AtomicBoolean disponivel,
                       ArrayList<ObjectOutputStream> listaOos, AtomicBoolean threadCorre, ArrayList<Informacoes> listaServidores) {
        this.socketCli = socketCli;
        this.ligacoesTCP = ligacoesTCP;
        this.dbName = dbName;
        this.disponivel = disponivel;
        this.listaOos = listaOos;
        this.threadCorre = threadCorre;
        this.listaServidores = listaServidores;
    }

    @Override
    public void run() {
        try {

            InputStream is = socketCli.getInputStream();
            OutputStream os = socketCli.getOutputStream();
            ObjectInputStream oisSocket = new ObjectInputStream(is);
            ObjectOutputStream oos = new ObjectOutputStream(os);

            Object msgSocket = oisSocket.readObject();
            Msg msgSockett = (Msg) msgSocket;

            if (msgSockett.getMsg() != null) {
                if (msgSockett.getMsg().equals("CloneBD")) {
                    System.out.println("[INFO] A clonar DataBase...");

                    FileInputStream fis = new FileInputStream(dbName);
                    byte[] bufferClient = new byte[4000];
                    int nBytes;
                    do {
                        nBytes = fis.read(bufferClient);
                        Msg msg = new Msg();
                        msg.setMsgBuffer(bufferClient);
                        msg.setMsgSize(nBytes);
                        if (nBytes == -1) {
                            msg.setMsgBuffer(new byte[4000]);
                            msg.setMsgSize(0);
                            msg.setLastPacket(true);
                        } else
                            msg.setLastPacket(false);
                        oos.reset();
                        oos.writeUnshared(msg);

                    } while (nBytes != -1);

                } else if (msgSockett.getMsg().equals("Cliente")) {
                    ligacoesTCP.getAndIncrement();

                    synchronized (listaOos) {
                        if (!listaOos.contains(oos)) {
                            listaOos.add(oos);

                            synchronized (listaServidores) {
                                Comparator<Informacoes> compare = new InformacoesComparator();
                                listaServidores.sort(compare);
                            }

                            for (ObjectOutputStream o : listaOos) {
                                enviaListaServidoresAtualizada(o);
                            }
                        }
                    }
                }
            }

            while (threadCorre.get()) {
                msgSocket = oisSocket.readObject();
                System.out.println("\nMsg do Cliente: " + msgSocket);

                if (msgSocket instanceof ArrayList) {
                    ArrayList<String> msgSockettt = (ArrayList<String>) msgSocket;
                    Msg msg = new Msg();

                    //Servidor.atualiza("Prepare",connDB.getVersao().get() + 1);

                    switch (msgSockettt.get(0)) {
                        case "REGISTA_USER" -> {
                            switch (connDB.insertUser(msgSockettt,true)) {
                                case ADMIN_NAO_PODE_REGISTAR -> {
                                    msg.setMsg("\nImpossível registar como admin");
                                }
                                case CLIENTE_REGISTADO_SUCESSO -> {
                                    msg.setMsg("\nCliente registado com sucesso!");

                                    //commit
                                }

                                case CLIENTE_JA_REGISTADO -> {
                                    msg.setMsg("\nCliente já registado!");
                                }
                            }
                        }
                        case "LOGIN_USER" -> {
                            String str = connDB.logaUser(msgSockettt,true);
                            msg.setMsg("\n" + str);
                        }
                        case "EDITA_NAME" -> {
                            String str = connDB.updateUser(msgSockettt,0,true);
                            msg.setMsg("\n" + str);
                        }
                        case "EDITA_USERNAME" -> {
                            String str = connDB.updateUser(msgSockettt, 1,true);
                            msg.setMsg("\n" + str);
                        }
                        case "EDITA_PASSWORD" -> {
                            String str = connDB.updateUser(msgSockettt, 2,true);
                            msg.setMsg("\n" + str);
                        }
                        case "INSERE_ESPETACULOS" -> {

                            String str = connDB.insereEspetaculos(msgSockettt,true);
                            msg.setMsg("\n" + str);
                        }
                        case "TORNA_VISIVEL" -> {
                            String str = connDB.tornaVisivel(msgSockettt,true);
                            msg.setMsg("\n" + str);
                        }
                        case "FILTRO_ESPETACULO"->{
                            String str = connDB.filtraEspetaculo(Integer.parseInt(msgSockettt.get(1)),msgSockettt.get(2), msgSockettt.get(3));
                            msg.setMsg("\n" + str);
                        }
                        case "SELECIONAR_ESPETACULO"->{
                            String str = connDB.selecionaEspetaculo(Integer.parseInt(msgSockettt.get(1)));
                            msg.setMsg("\n" + str);
                        }
                        case "SUBMETE_RESERVA"->{
                            String str = connDB.submeteReserva(msgSockettt,true);
                            msg.setMsg("\n" + str);
                        }
                        case "EFETUA_PAGAMENTO"->{
                            String str = connDB.efetuaPagamento(msgSockettt,true);
                            msg.setMsg("\n" + str);
                        }
                        case "LIMITE_TEMPO"->{
                            String str = connDB.retiraReservaLimiteTempo(msgSockettt,true);
                            msg.setMsg("\n" + str);
                        }
                        case "CONSULTA_RESERVAS_PAGAS"->{
                            String str = connDB.consultaReservasPagas(msgSockettt.get(1));
                            msg.setMsg("\n" + str);
                        }
                        case "CONSULTA_RESERVAS_PENDENTES"->{
                            String str = connDB.consultaReservasPendentes(msgSockettt.get(1));
                            msg.setMsg("\n" + str);
                        }
                        case "ELIMINA_ESPETACULO"->{
                            String str = connDB.eliminarEspetaculo(msgSockettt,true);
                            msg.setMsg("\n" + str);
                        }
                        case "LOGOUT"->{
                            String str = connDB.logout(msgSockettt,true);
                            msg.setMsg("\n" + str);
                        }

                    }
                    for (ObjectOutputStream oss: listaOos) {
                        oss.writeUnshared(msg);
                    }

                }
            }

        } catch (ClassNotFoundException e) {
            System.out.println("Classe Não encontrada");
        } catch (IOException e) {
            try {
                socketCli.close();
                //ligacoesTCP.getAndDecrement();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        System.out.println("[INFO] ComunicaTCP terminado com sucesso!");
    }

    void enviaListaServidoresAtualizada(ObjectOutputStream oos) {
        Msg msg = new Msg();
        try {
            synchronized (listaServidores) {

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

            }

        } catch (IOException e) {

        }
    }
}

