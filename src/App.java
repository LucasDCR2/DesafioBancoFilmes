//                                  javac -cp "lib/mysql-connector-java-8.0.33.jar:lib/gson-2.8.9.jar:src" src/*.java
//                                  java -cp "lib/mysql-connector-java-8.0.33.jar:lib/gson-2.8.9.jar:src" App

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Scanner;

public class App {
    public static void main(String[] args) throws SQLException, IOException {
        Scanner scanner = new Scanner(System.in);

        try {
            // Estabelecer a conexão com o banco de dados
            Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/netflix", "root", "implyroot2882");

            // Instanciar o gerenciador do Netflix
            Netflix netflix = new Netflix(connection, "81b11a8f239eb298141405cf23539139");
            netflix.criarTabelas();

            System.out.println("Bem-vindo ao MovieFlix! Você já possui um cadastro? (s/n)");
            String respostaCadastro = scanner.nextLine();

            String apelido = "";
            if (respostaCadastro.equalsIgnoreCase("s")) {
                System.out.println("Digite seu apelido:");
                apelido = scanner.nextLine();
            } else if (respostaCadastro.equalsIgnoreCase("n")) {
                System.out.println("Crie um apelido para sua conta:");
                apelido = scanner.nextLine();
                netflix.criarUsuario(apelido);
            } else {
                System.out.println("Opção inválida. Encerrando o programa.");
                System.exit(0);
            }

            boolean sair = false;
            while (!sair) {
                System.out.println("\nMenu Principal");
                System.out.println("1. Ver Catálogo");
                System.out.println("2. Ver Lista de Filmes para Assistir Depois");
                System.out.println("3. Sair");

                int opcao = scanner.nextInt();
                scanner.nextLine(); // Consumir a quebra de linha após o número

                switch (opcao) {
                    case 1:
                        netflix.verCatalogo();
                        break;
                    case 2:
                        netflix.verListaFilmes(apelido);
                        break;
                    case 3:
                        sair = true;
                        break;
                    default:
                        System.out.println("Opção inválida.");
                        break;
                }
            }

            scanner.close();
            connection.close(); // Fechar a conexão com o banco de dados

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

//                                  javac -cp "lib/mysql-connector-java-8.0.33.jar:lib/gson-2.8.9.jar:src" src/*.java
//                                  java -cp "lib/mysql-connector-java-8.0.33.jar:lib/gson-2.8.9.jar:src" App