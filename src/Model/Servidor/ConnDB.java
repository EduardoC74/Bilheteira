package Model.Servidor;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static Model.Servidor.Servidor.connDB;
import static Model.Servidor.Servidor.ms;

public class ConnDB
{
    private String DATABASE_URL;
    private Connection dbConn;
    private String dbName;
    private static AtomicInteger versaoDB;

    public ConnDB(String dBName) throws SQLException {//
        this.dbName = dBName;
        DATABASE_URL =  "jdbc:sqlite:" + dBName;
        dbConn = DriverManager.getConnection(DATABASE_URL);
        versaoDB = new AtomicInteger(1);
    }
    public boolean verificaDb() throws SQLException {
        System.out.println("[INFO] A verificar db existente...");
        ResultSet rs ;
        DatabaseMetaData meta = dbConn.getMetaData();
        rs = meta.getTables(null,null,null,new String[] {
                "TABLE"
        });
        int count = 0;
        while (rs.next()) {
            String tblName = rs.getString("TABLE_NAME");
            count++;
        }

        return count == 6;
    }
    public void criaTabelas()
    {
        System.out.println("[INFO] A criar tabela nova...");
        try {
            Statement statement = dbConn.createStatement();

            String sqlQueryEspetaculo = """
                    CREATE TABLE IF NOT EXISTS espetaculo
                    (id INTEGER NOT NULL,
                    descricao TEXT NOT NULL,
                    tipo TEXT NOT NULL,
                    data_hora TEXT NOT NULL,
                    duracao INTEGER NOT NULL,
                    local TEXT NOT NULL,
                    localidade TEXT NOT NULL,
                    pais TEXT NOT NULL,
                    classificacao_etaria TEXT NOT NULL,
                    visivel INTEGER NOT NULL)
                    """;

            String sqlQueryLugar = """
                    CREATE TABLE IF NOT EXISTS lugar
                    (id INTEGER NOT NULL,
                    fila TEXT NOT NULL,
                    assento TEXT NOT NULL,
                    preco REAL NOT NULL,
                    espetaculo_id INTEGER NOT NULL)
                    """;

            String sqlQueryReserva = """
                    CREATE TABLE IF NOT EXISTS reserva
                    (id INTEGER NOT NULL,
                    data_hora TEXT NOT NULL,
                    pago INTEGER NOT NULL,
                    id_utilizador INTEGER NOT NULL,
                    id_espetaculo INTEGER NOT NULL)""";

            String sqlQueryReserva_lugar = """
                    CREATE TABLE IF NOT EXISTS reserva_lugar
                    (id_reserva INTEGER NOT NULL,
                    id_lugar INTEGER NOT NULL)
                    """;

            String sqlQueryUtilizador = """
                    CREATE TABLE IF NOT EXISTS utilizador
                    (id INTEGER NOT NULL,
                    username TEXT NOT NULL,
                    nome TEXT NOT NULL,
                    password TEXT NOT NULL,
                    administrator INTEGER NOT NULL DEFAULT 0,
                    autenticado INTEGER NOT NULL DEFAULT 0 )
                    """;
            String sqlQueryVersao = """
                    CREATE TABLE IF NOT EXISTS versao_db
                    (versao INTEGER NOT NULL)
                    """;

            statement.executeUpdate(sqlQueryEspetaculo);
            statement.executeUpdate(sqlQueryLugar);
            statement.executeUpdate(sqlQueryReserva);
            statement.executeUpdate(sqlQueryReserva_lugar);
            statement.executeUpdate(sqlQueryUtilizador);
            statement.executeUpdate(sqlQueryVersao);
            statement.close();

            versaoDB = new AtomicInteger(1);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public MensagensRetorno insertUser(ArrayList<String> msgSockett,boolean isComunicaTCP) throws SQLException
    {
        String name = msgSockett.get(1);
        String username = msgSockett.get(2);
        String password = msgSockett.get(3);

        Statement statement = dbConn.createStatement();
        if (name != null && username != null && password != null) {
            if (username.equals("admin") && password.equals("admin")){
                return MensagensRetorno.ADMIN_NAO_PODE_REGISTAR;
            }
            String verifica = "SELECT * FROM utilizador WHERE nome='"+name+"' OR username='" + username + "'";
            ResultSet resultSet = statement.executeQuery(verifica);
            if (!resultSet.next()) {

                ResultSet r = statement.executeQuery("SELECT COUNT(*) FROM utilizador");
                int count = -1;
                if(r.next())
                    count = r.getInt(1);
                r.close();

                if(count > -1){
                    String sqlQuery = "INSERT INTO utilizador VALUES ('" + count + "','" + username + "','" + name + "','" + password + "','" + 0 + "','" + 0 + "')";
                    if(isComunicaTCP){
                        Servidor.atualiza("Prepare",connDB.getVersao().get() + 1, msgSockett);
                    }
                    statement.executeUpdate(sqlQuery);
                    incrementaVersao();
                    String versao = "UPDATE versao_db SET versao='"+getVersao().get() + "'";
                    statement.executeUpdate(versao);
                    statement.close();
                    return MensagensRetorno.CLIENTE_REGISTADO_SUCESSO;

                }
                else{

                    statement.close();
                    return MensagensRetorno.CLIENTE_JA_REGISTADO;
                }
            }
        }
        statement.close();
        return  MensagensRetorno.CLIENTE_JA_REGISTADO;
    }

    public void inicializa() throws SQLException {
        Statement statement = dbConn.createStatement();

        ResultSet rs = statement.executeQuery("SELECT * FROM versao_db");
        if (rs.next()) {
            int aux = rs.getInt(1);

            rs.close();

            setVersaoDB(aux);

            } else {
                String sqlQuery = "UPDATE utilizador SET autenticado='" + 0 + "' WHERE autenticado=" + 1;
                statement.executeUpdate(sqlQuery);
                ResultSet r = statement.executeQuery("SELECT COUNT(*) FROM utilizador");
                r.next();
                int count = r.getInt(1);
                r.close();

                String user = "INSERT INTO utilizador VALUES ('" + count + "','" + "admin" + "','" + "admin" + "','" + "admin" + "','" + 1 + "','" + 0 + "')";
                statement.executeUpdate(user);

                String versao = "INSERT INTO versao_db VALUES ('" + 1 + "')";
                statement.executeUpdate(versao);
            }
            statement.close();

    }

    public String logaUser(ArrayList<String> msgSockettt,boolean isComunicaTCP) throws SQLException
    {
        String username = msgSockettt.get(1);
        String password = msgSockettt.get(2);
        String login = null;
        Statement statement = dbConn.createStatement();
        String verificaExistente = "SELECT * FROM utilizador";
        if (username != null && password != null) {
            verificaExistente += " WHERE username = '" + username + "' AND password = '" + password + "'";
            ResultSet resultSet = statement.executeQuery(verificaExistente);
            int logado = resultSet.getInt("autenticado");
            if(logado == 1)
                return "Cliente com inicio de sessão já ativa!";


            if (!resultSet.next()) {
                login =  "Dados incorretos!";
            } else if (username.equals("admin") && password.equals("admin")){
                int id = resultSet.getInt("id");
                String sqlQuery = "UPDATE utilizador SET autenticado='" + 1 + "',administrator='" + 1 + "' WHERE id=" + id;

                if(isComunicaTCP)
                    Servidor.atualiza("Prepare", connDB.getVersao().get() + 1, msgSockettt);
                statement.executeUpdate(sqlQuery);
                login = "Login efetuado como admin com sucesso!";
                incrementaVersao();
                String versao = "UPDATE versao_db SET versao='"+getVersao().get() + "'";
                statement.executeUpdate(versao);
            }
            else {
                int id = resultSet.getInt("id");
                String sqlQuery = "UPDATE utilizador SET autenticado='" + 1 + "' WHERE id=" + id;

                login ="Login efetuado com sucesso!";
                if(isComunicaTCP)
                    Servidor.atualiza("Prepare", connDB.getVersao().get() + 1, msgSockettt);
                statement.executeUpdate(sqlQuery);
                incrementaVersao();
                String versao = "UPDATE versao_db SET versao='"+getVersao().get() + "'";
                statement.executeUpdate(versao);
            }
            resultSet.close();
        }

        statement.close();
        return login;
    }

    public String updateUser(ArrayList<String> msgSockettt,int tipo,boolean isComunicaTCP) throws SQLException
    {
        String atualizaCampo = msgSockettt.get(1);
        String id = msgSockettt.get(2);
        String update = null;
        Statement statement = dbConn.createStatement();
        String sqlQuery = null;
        
        switch (tipo) {
            case 0 -> {
               sqlQuery = "UPDATE utilizador SET nome='" + atualizaCampo + "' WHERE username='" + id + "'";
               update = "Nome mudado com sucesso!";

            }
            case 1 -> {
                sqlQuery = "UPDATE utilizador SET username='" + atualizaCampo + "' WHERE username='" + id + "'";
                update = "Username mudado com sucesso!";
            }
            case 2 -> {
                sqlQuery = "UPDATE utilizador SET password='" + atualizaCampo + "' WHERE username='" + id + "'";
                update = "Password mudada com sucesso!";
            }
        }

        if(isComunicaTCP)
            Servidor.atualiza("Prepare", connDB.getVersao().get() + 1, msgSockettt);
        statement.executeUpdate(sqlQuery);
        incrementaVersao();
        String versao = "UPDATE versao_db SET versao='"+getVersao().get() + "'";
        statement.executeUpdate(versao);

        statement.close();
        return update;
    }

    public AtomicInteger getVersao() {
        return versaoDB;
    }

    public void incrementaVersao() {
        versaoDB.getAndIncrement();
    }
    public void setVersaoDB(int versaoDB) {
        ConnDB.versaoDB.getAndSet(versaoDB);
    }
    public String getDbName() {
        return dbName;
    }

    public String insereEspetaculos(ArrayList<String> msgSockettt,boolean isComunicaTCP) {
        String insere = null;
        String fileName = msgSockettt.get(1);
        try (FileInputStream fstream = new FileInputStream(fileName)) {

            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

            String line = br.readLine();
            String data_hora;
            ArrayList<String> listaEspetaculos = new ArrayList<>();


            HashMap<String, HashMap<String, String>> fila = new HashMap<>();

            //ADICIONA TABELA ESPETACULOS
            while (line != null) {

                String[] attributes = line.split(";");
                switch (attributes.length) {
                    case 2 -> {
                        String[] auxiliar = attributes[1].split(":");
                        if(auxiliar.length < 2)
                            listaEspetaculos.add(attributes[1]);
                    }
                    case 4 -> {
                        data_hora = attributes[1] + "-" + attributes[2] + "-" + attributes[3] + " ";

                        line = br.readLine();
                        attributes = line.split(";");

                        data_hora += attributes[1] + ":" + attributes[2];
                        listaEspetaculos.add(data_hora);
                    }
                    default -> {

                        HashMap<String, String> lugares = new HashMap<>();


                        if (attributes[0].length() == 3) {
                            for (int i = 0; i < attributes.length; i++) { // corresponde a uma fila

                                String[] attributesAux = attributes[i].replace("\"", "").trim().split(":");

                                if (i == 0) {
                                    attributes[0] = attributesAux[0];
                                } else if (i == 1) {
                                    if (!fila.containsKey(attributes[0])) {
                                        lugares.put(attributesAux[0], attributesAux[1]);
                                        fila.put(attributes[0], lugares);
                                    }
                                } else {
                                    if (!fila.get(attributes[0]).containsKey(attributesAux[0])) {

                                        lugares.put(attributesAux[0], attributesAux[1]);

                                        fila.get(attributes[0]).putAll(lugares);
                                    }
                                }
                            }
                        }
                    }
                }
                line = br.readLine();
            }

            ArrayList<String> insereEspetaculosLista = new ArrayList<>();
            for (int i = 0; i < listaEspetaculos.size(); i++) {
                String result = listaEspetaculos.get(i).replaceAll("\"", "");
                insereEspetaculosLista.add(result);
            }


            Statement statement = connDB.dbConn.createStatement();

            String verificaExistente = "SELECT * FROM espetaculo";

            verificaExistente += " WHERE descricao = '" + insereEspetaculosLista.get(0)+ "' AND data_hora='" + insereEspetaculosLista.get(2)+"'";
            ResultSet resultSet = statement.executeQuery(verificaExistente);
            if (!resultSet.next()) {

                ResultSet r = statement.executeQuery("SELECT COUNT(*) FROM espetaculo");
                r.next();
                int count = r.getInt(1);
                r.close();

                String espetaculo = "INSERT INTO espetaculo VALUES ('" + count + "','" + insereEspetaculosLista.get(0) + "','" + insereEspetaculosLista.get(1) + "','" + insereEspetaculosLista.get(2) + "','" + insereEspetaculosLista.get(3) + "','"
                        + insereEspetaculosLista.get(4) + "','" + insereEspetaculosLista.get(5) + "','" + insereEspetaculosLista.get(6) + "','" + insereEspetaculosLista.get(7) + "','" + 0 + "')";

                statement.executeUpdate(espetaculo);

                for (var a: fila.keySet()) {
                    for (var b: fila.get(a).keySet()) {
                        ResultSet ra = statement.executeQuery("SELECT COUNT(*) FROM lugar");
                        r.next();
                        int count2 = ra.getInt(1);
                        r.close();
                        String insereLugar = "INSERT INTO lugar VALUES ('"+ count2 + "','"+ a +"','"+ b +"','"+ fila.get(a).get(b) +"','"+ count + "')" ;
                        statement.executeUpdate(insereLugar);
                    }
                }

                insere = "Espetáculo inserido com sucesso!";
                if(isComunicaTCP)
                    Servidor.atualiza("Prepare", connDB.getVersao().get() + 1, msgSockettt);
                incrementaVersao();
                String versao = "UPDATE versao_db SET versao='" + getVersao().get() + "'";
                statement.executeUpdate(versao);
            }
            statement.close();
        } catch (IOException ioe) {
            insere = "Este ficheiro não existe!";
        } catch (Exception e) {
            insere = "Ficheiro inválido!";
        }
        return insere;
    }

    public String tornaVisivel(ArrayList<String> msgSockettt,boolean isComunicaTCP) throws SQLException {
        int idEspetaculo = Integer.parseInt(msgSockettt.get(1));
        Statement statement = connDB.dbConn.createStatement();
        ResultSet r = statement.executeQuery("SELECT * FROM espetaculo WHERE id =" + idEspetaculo);
        if (r.next()) {
            String sqlQuery = "UPDATE espetaculo SET visivel='" + 1 + "' WHERE id=" + idEspetaculo;
            if(isComunicaTCP)
                Servidor.atualiza("Prepare", connDB.getVersao().get() + 1, msgSockettt);
            statement.executeUpdate(sqlQuery);
            incrementaVersao();
            String versao = "UPDATE versao_db SET versao='" + getVersao().get() + "'";
            statement.executeUpdate(versao);
            return "Espetáculo visível!";
        }
    return "Espetáculo não existe!";
    }

    public String filtraEspetaculo(int i, String filtro, String username)throws SQLException {

        Statement statement = connDB.dbConn.createStatement();
        ResultSet r = null;
        
        switch (i) {
            case 0 -> r = statement.executeQuery("SELECT * FROM espetaculo WHERE descricao like '%" + filtro+ "%'");
            case 1 -> r = statement.executeQuery("SELECT * FROM espetaculo WHERE tipo like '%" + filtro+ "%'");
            case 2 -> r = statement.executeQuery("SELECT * FROM espetaculo WHERE data_hora like '%" + filtro+ "%'");
            case 3 -> r = statement.executeQuery("SELECT * FROM espetaculo WHERE duracao like '%" + filtro+ "%'");
            case 4 -> r = statement.executeQuery("SELECT * FROM espetaculo WHERE local like '%" + filtro+ "%'");
            case 5 -> r = statement.executeQuery("SELECT * FROM espetaculo WHERE localidade like '%" + filtro+ "%'");
            case 6 -> r = statement.executeQuery("SELECT * FROM espetaculo WHERE pais like '%" + filtro+ "%'");
            case 7 -> r = statement.executeQuery("SELECT * FROM espetaculo WHERE classificacao_etaria like '%" + filtro+ "%'");
            case 8 -> r = statement.executeQuery("SELECT * FROM espetaculo");
        }

        StringBuilder sb = new StringBuilder();
        StringBuilder sbs = new StringBuilder();

        while (r.next()) {
            int visibilidade = r.getInt("visivel");
            if(visibilidade == 0 && !username.equalsIgnoreCase("admin"))
                continue;

            int numero = r.getInt("id");
            String descricao = r.getString("descricao");

            String tipo = r.getString("tipo");
            String data_hora = r.getString("data_hora");
            int duracao = r.getInt("duracao");
            String local = r.getString("local");
            String localidade = r.getString("localidade");
            String pais = r.getString("pais");
            String classificacao = r.getString("classificacao_etaria");
            sbs.append("\n\nNúmero do Espetáculo: "+numero).append("\nDescrição: "+descricao).append("\nTipo: "+tipo).append("\nData: "+data_hora).append("\nDuracao: "+duracao).append("\nLocal: "+local)
                    .append("\nLocalidade: "+localidade).append("\nPais: "+pais).append("\nClassificação: "+classificacao);

        }
        if(sbs.length() != 0){
            sb = sbs;
        }else sb.append("Não existe espetáculo com esse filtro!");
        return sb.toString();
    }

    public String selecionaEspetaculo(int idEspetaculo) throws SQLException {
        LocalDateTime now = LocalDateTime.now();
        Statement statement = connDB.dbConn.createStatement();
        ResultSet r = statement.executeQuery("SELECT * FROM espetaculo WHERE id =" + idEspetaculo);
        if (r.next()) {
            String data_hora = r.getString("data_hora");

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
            LocalDateTime hora_espetaculo = LocalDateTime.parse(data_hora, formatter);

            long hora = ChronoUnit.HOURS.between(now, hora_espetaculo);

            if (hora < 24)
                return "Não é possivel selecionar este espetáculo!";

            int visibilidade = r.getInt("visivel");
            if (visibilidade == 0)
                return "Não é possivel selecionar este espetáculo!";


            ResultSet rL = statement.executeQuery("SELECT * FROM lugar where espetaculo_id="+idEspetaculo);
            StringBuilder sb = new StringBuilder();
            String filaaux = null;
            int muda = 0;
            int c = 0;

            while (rL.next()) {
                String fila = rL.getString("fila");

                if(filaaux == null) {
                    filaaux = fila;
                    muda = 0;
                }

                String assento = rL.getString("assento");
                String preco = rL.getString("preco");
                if(!filaaux.equals(fila)) {
                    sb.append("\n");
                    filaaux = null;
                    muda = 1;
                }

                if(muda == 1 || c == 0){
                    sb.append("|"+fila+"| ");
                }

                String query = "SELECT id from lugar WHERE fila ='" + fila + "' AND assento='" + assento + "' AND espetaculo_id=" + idEspetaculo; // devolve a linha com o lugar
                Statement st = dbConn.createStatement();
                ResultSet resultSet = st.executeQuery(query);
                if (resultSet.next()) {
                    int id_lugar = resultSet.getInt("id");
                    ResultSet rs =  st.executeQuery("SELECT COUNT(*) FROM reserva_lugar WHERE id_lugar=" + id_lugar);
                    rs.next();
                    int count = rs.getInt(1);
                    rs.close();

                    if (count != 0) {
                        preco = " X  ";
                    }
                }
                st.close();
                resultSet.close();

                sb.append(assento+" -> ").append(preco+" | ");
                c++;
            }

            rL.close();

            return sb.toString();
        }
        return "Espetáculo Inexistente!";
    }

    public String submeteReserva(ArrayList<String> msgSockettt,boolean isComunicaTCP) throws SQLException {
        StringBuilder submete = new StringBuilder();
        Statement st = dbConn.createStatement();

        String username = msgSockettt.get(msgSockettt.size() - 1);
        ResultSet r = null;
        int count = 0;
        for (int i = 2; i < msgSockettt.size() - 1; i++) {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
            String data_hora = now.format(dateTimeFormatter);
            //comParts[0] -> fila || comParts[1] -> lugar
            String[] comParts = msgSockettt.get(i).toUpperCase().split("-");

            int id_espetaculo = Integer.parseInt(msgSockettt.get(1));
            String query = "SELECT id from lugar where fila ='" + comParts[0] + "' AND assento='" + comParts[1] + "' AND espetaculo_id=" + id_espetaculo; // devolve a linha com o lugar
            r = st.executeQuery(query);

            //lugares existem
            if (r.next()) {
                int id_lugar = r.getInt(1);
                ResultSet rs =  st.executeQuery("SELECT id_reserva FROM reserva_lugar WHERE id_lugar=" + id_lugar);
                //int count = -1;

                //se lugar ja tiver reserva asscociada


                //se lugar não tiver reserva asscociada
                if (!rs.next()) {
                    String queryUsername = "SELECT id FROM utilizador WHERE username= '" + username + "'";
                    ResultSet rsUser = st.executeQuery(queryUsername);
                    if (rsUser.next()) {
                        int idUser = rsUser.getInt(1);

                        ResultSet resultSet = st.executeQuery("SELECT * FROM reserva where id_utilizador =" + idUser +
                                " AND id_espetaculo=" + id_espetaculo);


                        //se utilizador não tiver reservas naquele espetaculo
                        if (!resultSet.next() || i == 2) {

                            ResultSet rs1 = st.executeQuery("SELECT COUNT(*) FROM reserva");
                            if(rs1.next())
                                count = rs1.getInt(1);
                            rs1.close();

                            if(isComunicaTCP)
                                Servidor.atualiza("Prepare", connDB.getVersao().get() + 1, msgSockettt);
                            incrementaVersao();
                            String versao = "UPDATE versao_db SET versao='" + getVersao().get() + "'";
                            st.executeUpdate(versao);

                            String sqlQuery = "INSERT INTO reserva VALUES('"+count+"','" + data_hora + "','" + 0
                                    + "',(SELECT id FROM utilizador WHERE username= '" + username + "'),'" + id_espetaculo + "')";

                            st.executeUpdate(sqlQuery);

                            String sqlQuery2 = "INSERT INTO reserva_lugar VALUES( '"+count+"','" + id_lugar + "')";

                            st.executeUpdate(sqlQuery2);

                            //se utilizador tiver reservas naquele espetaculo
                        } else {
                            int id = count;

                            String sqlQuery2 = "INSERT INTO reserva_lugar VALUES('" + id + "','" + id_lugar + "')";
                            if(isComunicaTCP)
                                Servidor.atualiza("Prepare", connDB.getVersao().get() + 1, msgSockettt);
                            st.executeUpdate(sqlQuery2);
                            incrementaVersao();
                            String versao = "UPDATE versao_db SET versao='" + getVersao().get() + "'";
                            st.executeUpdate(versao);
                        }

                        submete.append("\nLugar reservado com sucesso: [" + comParts[0] + "-" + comParts[1] + "]");
                    }
                } else {
                    submete.append("\nLugar já reservado! [" + comParts[0] + "-" + comParts[1] + "]");
                }
            } else {
                submete.append("\nLugar não existe! [" + comParts[0] + "-" + comParts[1] + "]");
            }
        }
        r.close();
        st.close();
        return String.valueOf(submete);
    }

    public String efetuaPagamento(ArrayList<String> msgSockettt,boolean isComunicaTCP) throws SQLException {
        String username = msgSockettt.get(1);
        Statement st= dbConn.createStatement();
        String verificaExistente = "SELECT * FROM utilizador WHERE username = '" + username + "'";
        ResultSet rs = st.executeQuery(verificaExistente);
        if(rs.next()){
            int idUser = rs.getInt("id");
            String verificaIdReserva = "SELECT * FROM reserva WHERE id_utilizador="+idUser;
            ResultSet resultSet = st.executeQuery(verificaIdReserva);
            if(resultSet.next()){
                if(isComunicaTCP)
                    Servidor.atualiza("Prepare", connDB.getVersao().get() + 1, msgSockettt);
                incrementaVersao();
                String versao = "UPDATE versao_db SET versao='" + getVersao().get() + "'";
                st.executeUpdate(versao);
                st.executeUpdate("UPDATE reserva SET pago='"+1+"' WHERE pago='"+0+"' AND id_utilizador="+idUser);
                rs.close();
                resultSet.close();
                return "Reserva paga com sucesso!";
            }
            return "Reservas inexistentes!";
        }


        return "UserName inexistente!";
    }

    public String retiraReservaLimiteTempo(ArrayList<String> msgSockettt,boolean isComunicaTCP) throws SQLException {
        String username = msgSockettt.get(1);
        Statement st= dbConn.createStatement();
        String verificaExistente = "SELECT * FROM utilizador WHERE username = '" + username + "'";
        ResultSet rs = st.executeQuery(verificaExistente);
        if(rs.next()){
            int idUser = rs.getInt("id");
            String verificaIdReserva = "SELECT * FROM reserva WHERE id_utilizador='"+idUser+"' AND pago=" + 0;
            ResultSet resultSet = st.executeQuery(verificaIdReserva);
            if(resultSet.next()){
                int idReserva = resultSet.getInt("id");
                if(isComunicaTCP)
                    Servidor.atualiza("Prepare", connDB.getVersao().get() + 1, msgSockettt);
                incrementaVersao();
                String versao = "UPDATE versao_db SET versao='" + getVersao().get() + "'";
                st.executeUpdate(versao);
                st.executeUpdate("DELETE FROM reserva_lugar WHERE id_reserva="+idReserva);
                st.executeUpdate("DELETE FROM reserva WHERE pago='"+0+"' AND id="+idReserva);
                return "Reserva eliminada com sucesso!";
            }
            return "Não tem reserva associada a esse username!";
        }
        return "UserName inexistente!";

    }

    public String consultaReservasPagas(String username) throws SQLException {
        StringBuilder sb = new StringBuilder();

        Statement st = dbConn.createStatement();
        String verificaExistente = "SELECT * FROM utilizador WHERE username = '" + username + "'";
        ResultSet rs = st.executeQuery(verificaExistente);

        //user existe
        if (rs.next()) {
            Statement stat = dbConn.createStatement();
            int idUser = rs.getInt("id");
            String verificaReserva = "SELECT * FROM reserva WHERE pago= '" + 1 + "' AND id_utilizador=" + idUser;
            ResultSet rs1 = stat.executeQuery(verificaReserva);

            //utilizador tem reservas pagas
            while (rs1.next()) {
                int idReserva = rs1.getInt("id");
                int idEspetaculo = rs1.getInt("id_espetaculo");
                Statement stats = dbConn.createStatement();
                String verificaEspetaculo = "SELECT * FROM espetaculo WHERE id=" + idEspetaculo;
                ResultSet rs2 = stats.executeQuery(verificaEspetaculo);

                //reserva tem espetaculo
                if(rs2.next()) {
                    int visibilidade = rs2.getInt("visivel");
                    if (visibilidade == 0)
                        continue;

                    int numero = rs2.getInt("id");
                    String descricao = rs2.getString("descricao");
                    String tipo = rs2.getString("tipo");
                    String data_hora2 = rs2.getString("data_hora");
                    int duracao = rs2.getInt("duracao");
                    String local = rs2.getString("local");
                    String localidade = rs2.getString("localidade");
                    String pais = rs2.getString("pais");
                    String classificacao = rs2.getString("classificacao_etaria");
                    sb.append("\n\nNúmero do Espetáculo: " + numero).append("\nDescrição: " + descricao).append("\nTipo: " + tipo).append("\nData: " + data_hora2).append("\nDuracao: " + duracao).append("\nLocal: " + local)
                            .append("\nLocalidade: " + localidade).append("\nPais: " + pais).append("\nClassificação: " + classificacao);

                    String verificaReservaLugares = "SELECT * FROM reserva_lugar WHERE id_reserva=" + idReserva;
                    Statement state = dbConn.createStatement();
                    ResultSet rs3 = state.executeQuery(verificaReservaLugares);
                    //reserva tem lugares
                    while (rs3.next()) {

                        int idLugar = rs3.getInt("id_lugar");


                        String verificaLugares = "SELECT * FROM lugar WHERE id=" + idLugar;
                        Statement statem = dbConn.createStatement();
                        ResultSet rs4 = statem.executeQuery(verificaLugares);

                        //lugares
                        if(rs4.next()) {
                            String fila = rs4.getString("fila");
                            String assento = rs4.getString("assento");

                            sb.append("\nFila: " + fila).append(" | Assento: " + assento);
                        }
                        rs4.close();
                    }
                    rs3.close();
                }
                rs2.close();

            }
            rs1.close();
        }
        rs.close();
        if(sb.isEmpty())
            return "Não existem reservas pagas!";
        return sb.toString();
    }

    public String consultaReservasPendentes(String username) throws SQLException {
        StringBuilder sb = new StringBuilder();

        Statement st = dbConn.createStatement();
        String verificaExistente = "SELECT * FROM utilizador WHERE username = '" + username + "'";
        ResultSet rs = st.executeQuery(verificaExistente);

        //user existe
        if (rs.next()) {
            Statement stat = dbConn.createStatement();
            int idUser = rs.getInt("id");
            String verificaReserva = "SELECT * FROM reserva WHERE pago= '" + 0 + "' AND id_utilizador=" + idUser;
            ResultSet rs1 = stat.executeQuery(verificaReserva);

            //utilizador tem reservas pagas
            while (rs1.next()) {
                int idReserva = rs1.getInt("id");
                int idEspetaculo = rs1.getInt("id_espetaculo");
                Statement stats = dbConn.createStatement();
                String verificaEspetaculo = "SELECT * FROM espetaculo WHERE id=" + idEspetaculo;
                ResultSet rs2 = stats.executeQuery(verificaEspetaculo);

                //reserva tem espetaculo
                if(rs2.next()) {
                    int visibilidade = rs2.getInt("visivel");
                    if (visibilidade == 0)
                        continue;

                    int numero = rs2.getInt("id");
                    String descricao = rs2.getString("descricao");
                    String tipo = rs2.getString("tipo");
                    String data_hora2 = rs2.getString("data_hora");
                    int duracao = rs2.getInt("duracao");
                    String local = rs2.getString("local");
                    String localidade = rs2.getString("localidade");
                    String pais = rs2.getString("pais");
                    String classificacao = rs2.getString("classificacao_etaria");
                    sb.append("\n\nNúmero do Espetáculo: " + numero).append("\nDescrição: " + descricao).append("\nTipo: " + tipo).append("\nData: " + data_hora2).append("\nDuracao: " + duracao).append("\nLocal: " + local)
                            .append("\nLocalidade: " + localidade).append("\nPais: " + pais).append("\nClassificação: " + classificacao);

                    String verificaReservaLugares = "SELECT * FROM reserva_lugar WHERE id_reserva=" + idReserva;
                    Statement state = dbConn.createStatement();
                    ResultSet rs3 = state.executeQuery(verificaReservaLugares);
                    //reserva tem lugares
                    while (rs3.next()) {

                        int idLugar = rs3.getInt("id_lugar");


                        String verificaLugares = "SELECT * FROM lugar WHERE id=" + idLugar;
                        Statement statem = dbConn.createStatement();
                        ResultSet rs4 = statem.executeQuery(verificaLugares);

                        //lugares
                        if(rs4.next()) {
                            String fila = rs4.getString("fila");
                            String assento = rs4.getString("assento");

                            sb.append("\nFila: " + fila).append(" | Assento: " + assento);
                        }
                        rs4.close();
                    }
                    rs3.close();
                }
                rs2.close();

            }
            rs1.close();
        }
        rs.close();
        return sb.toString();
    }

    public String eliminarEspetaculo(ArrayList<String> msgSockettt,boolean isComunicaTCP) throws SQLException {
        int idEspetaculo = Integer.parseInt(msgSockettt.get(1));
        Statement st = dbConn.createStatement();

        String verificaidEspetaculo = "SELECT id FROM espetaculo WHERE id=" + idEspetaculo;
        ResultSet resultSet = st.executeQuery(verificaidEspetaculo); // tem espetaculo pedido

        //espetaculo existe
        if (resultSet.next()) {
            //Statement stat = dbConn.createStatement();
            String verificaReserva = "SELECT * FROM reserva WHERE id_espetaculo=" + idEspetaculo;
            ResultSet rs1 = st.executeQuery(verificaReserva);
            if (!rs1.next()) {
                if(isComunicaTCP)
                    Servidor.atualiza("Prepare", connDB.getVersao().get() + 1, msgSockettt);
                incrementaVersao();
                String versao = "UPDATE versao_db SET versao='" + getVersao().get() + "'";
                st.executeUpdate(versao);
                st.executeUpdate("DELETE FROM lugar WHERE espetaculo_id=" + idEspetaculo);
                st.executeUpdate("DELETE FROM espetaculo WHERE id=" + idEspetaculo);
            } else {
                String verificaReservaPago = "SELECT * FROM reserva WHERE pago='" + 0 + "' AND id_espetaculo=" + idEspetaculo;
                ResultSet rs2 = st.executeQuery(verificaReservaPago);
                if(rs2.next()) {
                    int idReserva = rs2.getInt("id");
                    if(isComunicaTCP)
                        Servidor.atualiza("Prepare", connDB.getVersao().get() + 1, msgSockettt);
                    incrementaVersao();
                    String versao = "UPDATE versao_db SET versao='" + getVersao().get() + "'";
                    st.executeUpdate(versao);
                    st.executeUpdate("DELETE FROM reserva_lugar WHERE id_reserva=" + idReserva);
                    //int idReserva = rs1.getInt("id");
                    st.executeUpdate("DELETE FROM reserva WHERE id=" + idReserva);
                    st.executeUpdate("DELETE FROM lugar WHERE espetaculo_id=" + idEspetaculo);
                    st.executeUpdate("DELETE FROM espetaculo WHERE id=" + idEspetaculo);

                } else
                    return "Não é possível eliminar o espetáculo!";
            }
            return "Espetáculo eliminado com sucesso!";

        }
        return "Não é possível eliminar espetáculo inexistente!";
    }

    public String logout(ArrayList<String> msgSockettt,boolean isComunicaTCP) throws SQLException {
        String username = msgSockettt.get(1);
        Statement st = dbConn.createStatement();
        String user = "SELECT * FROM utilizador WHERE username='" + username + "'";
        ResultSet rs = st.executeQuery(user);
        if(rs.next()) {
            if(isComunicaTCP){
                Servidor.atualiza("Prepare",connDB.getVersao().get() + 1, msgSockettt);
            }
            String logout = "UPDATE utilizador SET autenticado ='" + 0 + "' WHERE username='" + username + "'";
            st.executeUpdate(logout);
            incrementaVersao();

            String versao = "UPDATE versao_db SET versao='" + getVersao().get() + "'";
            st.executeUpdate(versao);
        }

        return "Cliente terminou sessão!";
    }


}
