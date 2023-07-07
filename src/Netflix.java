//                                      javac -cp "lib/mysql-connector-java-8.0.33.jar:lib/gson-2.8.9.jar:src" src/*.java
//                                      java -cp "lib/mysql-connector-java-8.0.33.jar:lib/gson-2.8.9.jar:src" App


//========================================================================< Imports >==========================================================//

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonArray;


//=====================================================================< Classe Netflix >=======================================================//


public class Netflix {
    private Connection connection;
    private String tmdbApiKey;
    private List<Filme> catalogo;

    public Netflix(Connection connection, String tmdbApiKey) {
        this.connection = connection;
        this.tmdbApiKey = tmdbApiKey;
        this.catalogo = new ArrayList<>();
    }


//======================================================================< Criar Tabelas >=======================================================//


    public void criarTabelas() throws SQLException {
        String tabelaUsuarios = "CREATE TABLE IF NOT EXISTS usuarios (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "apelido VARCHAR(50) NOT NULL," +
                "INDEX idx_usuarios_apelido (apelido)" +
                ")";

        String tabelaCatalogo = "CREATE TABLE IF NOT EXISTS catalogo (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "nome VARCHAR(100) NOT NULL" +
                ")";

        String tabelaListaFilmes = "CREATE TABLE IF NOT EXISTS lista_filmes (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "apelido VARCHAR(50) NOT NULL," +
                "id_filme INT NOT NULL," +
                "FOREIGN KEY (apelido) REFERENCES usuarios(apelido)," +
                "FOREIGN KEY (id_filme) REFERENCES catalogo(id)" +
                ")";

        Statement statement = connection.createStatement();
        statement.executeUpdate(tabelaUsuarios);
        statement.executeUpdate(tabelaCatalogo);
        statement.executeUpdate(tabelaListaFilmes);
        statement.close();

        System.out.println("Tabelas criadas com sucesso!");
    }


//======================================================================< Criar Usuario >======================================================//


    public void criarUsuario(String apelido) throws SQLException {
        String query = "INSERT INTO usuarios (apelido) VALUES (?)";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, apelido);
        statement.executeUpdate();
        System.out.println("Usuario criado com sucesso!");
    }

    
//=======================================================================< Ver catalogo >=====================================================//


    public void verCatalogo() throws SQLException, IOException {
        // Obter o catálogo de filmes da API
        JsonArray filmesArray = obterCatalogoFilmes();

        System.out.println("Catalogo de Filmes:");

        int count = 1; // Contador para o ID sequencial

        for (JsonElement filmeElement : filmesArray) {
            JsonObject filmeJson = filmeElement.getAsJsonObject();
            String title = filmeJson.get("title").getAsString();

            System.out.println("Indice: " + count + ", Titulo: " + title);

            // Armazenar o filme na tabela "catalogo"
            inserirFilmeCatalogo(count, title);

            count++; // Incrementar o contador de ID
        }
    }


//=======================================================================< Obter Filmes >=====================================================//


    private JsonArray obterCatalogoFilmes() throws IOException {
        String url = "https://api.themoviedb.org/3/discover/movie?api_key=" + tmdbApiKey;

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");

        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();

        Gson gson = new Gson();
        JsonObject catalogJson = gson.fromJson(response.toString(), JsonObject.class);
        JsonArray filmesArray = catalogJson.getAsJsonArray("results");

        return filmesArray;
    }


//======================================================================< Inserir Filmes >===================================================//


    private void inserirFilmeCatalogo(int id, String title) throws SQLException {
        String insertQuery = "INSERT INTO catalogo (id, nome) VALUES (?, ?)";
        PreparedStatement insertStatement = connection.prepareStatement(insertQuery);
        insertStatement.setInt(1, id);
        insertStatement.setString(2, title);
        insertStatement.executeUpdate();
        insertStatement.close();
    }


//======================================================================< Add Filmes Lista >==================================================//


    public void adicionarFilmeLista(String apelido) throws SQLException {
        Scanner scanner = new Scanner(System.in);
    
        // Exibir o catálogo de filmes
        System.out.println("Catalogo de Filmes:");
        exibirCatalogo();
    
        System.out.println("\nDigite o numero do filme que deseja adicionar a lista:");
        int indiceFilme = scanner.nextInt();
        scanner.nextLine(); // Consumir a quebra de linha após o número
    
        // Verificar se o índice do filme é válido
        if (indiceFilme >= 1 && indiceFilme <= catalogo.size()) {
            Filme filmeSelecionado = catalogo.get(indiceFilme - 1);
            int idFilme = filmeSelecionado.getId();
    
            // Verificar se o filme já está na lista do usuário
            if (verificarFilmeLista(apelido, idFilme)) {
                System.out.println("Este filme ja está na sua lista de filmes para assistir depois.");
            } else {
                // Adicionar o filme à tabela "lista_filmes"
                String insertQuery = "INSERT INTO lista_filmes (apelido, id_filme) VALUES (?, ?)";
                PreparedStatement insertStatement = connection.prepareStatement(insertQuery);
                insertStatement.setString(1, apelido);
                insertStatement.setInt(2, idFilme);
                insertStatement.executeUpdate();
                insertStatement.close();
    
                System.out.println("Filme \"" + filmeSelecionado.getTitulo() + "\" adicionado a sua lista de filmes para assistir depois.");
            }
        } else {
            System.out.println("Indice de filme invalido.");
        }
    
        scanner.close();
    }
    
    public void exibirCatalogo() throws SQLException {
        catalogo.clear(); // Limpar o catálogo atual
    
        String query = "SELECT id, nome FROM catalogo";
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(query);
    
        int count = 1;
        while (resultSet.next()) {
            int id = resultSet.getInt("id");
            String titulo = resultSet.getString("nome");
    
            Filme filme = new Filme(id, titulo);
            catalogo.add(filme);
    
            System.out.println("Indice: " + count + ", Titulo: " + titulo);
            count++;
        }
    
        statement.close();
        resultSet.close();
    }
    
    public boolean verificarFilmeLista(String apelido, int idFilme) throws SQLException {
        String query = "SELECT * FROM lista_filmes WHERE apelido = ? AND id_filme = ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, apelido);
        statement.setInt(2, idFilme);
        ResultSet resultSet = statement.executeQuery();
    
        boolean filmeExiste = resultSet.next();
    
        resultSet.close();
        statement.close();
    
        return filmeExiste;
    }
    

//========================================================================< Ver Filmes >=====================================================//


    public void verListaFilmes(String apelido) throws SQLException {
        String query = "SELECT catalogo.id, catalogo.nome " +
                "FROM catalogo " +
                "INNER JOIN lista_filmes ON catalogo.id = lista_filmes.id_filme " +
                "WHERE lista_filmes.apelido = ?";

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, apelido);
        ResultSet resultSet = statement.executeQuery();

        System.out.println("Lista de Filmes para Assistir Depois:");
        while (resultSet.next()) {
            int idFilme = resultSet.getInt("id");
            String nomeFilme = resultSet.getString("nome");
            System.out.println("ID: " + idFilme + ", Título: " + nomeFilme);
        }

        statement.close();
    }
}


//============================================================================< Fim >======================================================//


//                                 javac -cp "lib/mysql-connector-java-8.0.33.jar:lib/gson-2.8.9.jar:src" src/*.java
//                                 java -cp "lib/mysql-connector-java-8.0.33.jar:lib/gson-2.8.9.jar:src" App
