package Model.Servidor;

import utils.Msg;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

public class ListenUDP extends Thread {
    DatagramSocket ds;
    ArrayList<Informacoes> listaServidores;
    AtomicBoolean threadCorre;

    public ListenUDP(DatagramSocket ds , ArrayList<Informacoes> listaServidores, AtomicBoolean threadCorre){
        this.ds = ds;
        this.listaServidores = listaServidores;
        this.threadCorre = threadCorre;
    }

    @Override
    public void run() {
        while (threadCorre.get()){
            try{
                DatagramPacket dp = new DatagramPacket(new byte[256], 256); // recebe nome
                ds.receive(dp);

                ByteArrayInputStream bais = new ByteArrayInputStream(dp.getData());
                ObjectInputStream ois = new ObjectInputStream(bais);

                Msg msg = (Msg) ois.readObject();
                System.out.println("Client Connected[" + msg.getIp()+"]");

                Msg msgTCP = new Msg();
                msgTCP.setLastPacket(false);

                Iterator<Informacoes> iterator = listaServidores.iterator();
                while (iterator.hasNext()){
                    Informacoes info = iterator.next();

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ObjectOutputStream oos = new ObjectOutputStream(baos);

                    msgTCP.setPortoServer(info.getPorto());
                    msgTCP.setIp(info.getIp());
                    if(!iterator.hasNext()){
                        msgTCP.setLastPacket(true);
                    }


                    msgTCP.setLigacoesTCP(info.getLigacoes());
                    msg.setIndex(listaServidores.indexOf(info));


                    oos.writeUnshared(msgTCP);
                    byte[] noCache = baos.toByteArray();
                    dp.setData(noCache, 0,noCache.length);
                    ds.send(dp);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        }
}
