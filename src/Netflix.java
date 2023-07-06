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

public class Netflix {
    private Connection connection;
    private String tmdbApiKey;

    public Netflix(Connection connection, String tmdbApiKey) {
        this.connection = connection;
        this.tmdbApiKey = tmdbApiKey;
    }

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

    public void criarUsuario(String apelido) throws SQLException {
        String query = "INSERT INTO usuarios (apelido) VALUES (?)";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, apelido);
        statement.executeUpdate();
        System.out.println("Usuário criado com sucesso!");
    }

    public void verCatalogo(String apelido) throws SQLException, IOException {
        int movieId = 550; // Exemplo: filme "Fight Club"
    
        String url = "https://api.themoviedb.org/3/movie/" + movieId + "?api_key=" + tmdbApiKey;
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
    
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
    
        // Parse a resposta JSON e obtenha os dados relevantes do filme
        Gson gson = new Gson();
        JsonObject movieJson = gson.fromJson(response.toString(), JsonObject.class);
        String title = movieJson.get("title").getAsString();
        String overview = movieJson.get("overview").getAsString();
        String releaseDate = movieJson.get("release_date").getAsString();
    
        // Exiba as informações do filme para o usuário
        System.out.println("Detalhes do Filme:");
        System.out.println("Título: " + title);
        System.out.println("Sinopse: " + overview);
        System.out.println("Data de Lançamento: " + releaseDate);
    }

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

// javac -cp "lib/mysql-connector-java-8.0.33.jar:lib/gson-2.8.9.jar:src" src/*.java
// java -cp "lib/mysql-connector-java-8.0.33.jar:lib/gson-2.8.9.jar:src" App
// ou 0.28