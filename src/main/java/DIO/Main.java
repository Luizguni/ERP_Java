import javax.swing.*;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

// ==============================================================================
// 1. CLASSES DO MODELO (Dados e Lógica de Negócio)
//    - Representam as entidades do sistema: Cliente, Produto, ItemPedido e Pedido.
// ==============================================================================

/**
 * Representa a entidade Cliente.
 * Inclui campos para informações de contato e endereço.
 * Implementa Serializable para permitir o uso em coleções.
 */
class Cliente implements Serializable {
    private long id;
    private String nome;
    private String email;
    private String telefone;
    private String endereco;
    private String cidade;
    private String estado;
    private String pais;

    public Cliente(String nome, String email, String telefone, String endereco, String cidade, String estado, String pais) {
        this.nome = nome;
        this.email = email;
        this.telefone = telefone;
        this.endereco = endereco;
        this.cidade = cidade;
        this.estado = estado;
        this.pais = pais;
    }

    public Cliente(long id, String nome, String email, String telefone, String endereco, String cidade, String estado, String pais) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.telefone = telefone;
        this.endereco = endereco;
        this.cidade = cidade;
        this.estado = estado;
        this.pais = pais;
    }

    // Getters para acessar os atributos privados
    public long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public String getTelefone() {
        return telefone;
    }

    public String getEndereco() {
        return endereco;
    }

    public String getCidade() {
        return cidade;
    }

    public String getEstado() {
        return estado;
    }

    public String getPais() {
        return pais;
    }

    // Setter para o ID, útil para quando o banco de dados gera o ID
    public void setId(long id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Cliente cliente = (Cliente) o;
        return id == cliente.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return nome;
    }
}

/**
 * Representa a entidade Produto.
 * Contém um ID, nome e preço.
 */
class Produto implements Serializable {
    private long id;
    private String nome;
    private double preco;

    public Produto(String nome, double preco) {
        this.nome = nome;
        this.preco = preco;
    }

    public Produto(long id, String nome, double preco) {
        this.id = id;
        this.nome = nome;
        this.preco = preco;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public double getPreco() {
        return preco;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Produto produto = (Produto) o;
        return id == produto.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return nome + " (R$ " + String.format("%.2f", preco) + ")";
    }
}

/**
 * Representa um item dentro de um pedido, com um produto e a quantidade.
 */
class ItemPedido implements Serializable {
    private Produto produto;
    private int quantidade;

    public ItemPedido(Produto produto, int quantidade) {
        this.produto = produto;
        this.quantidade = quantidade;
    }

    public Produto getProduto() {
        return produto;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public double getSubtotal() {
        return quantidade * produto.getPreco();
    }
}

/**
 * Representa a entidade Pedido.
 * Contém o cliente associado e uma lista de itens.
 */
class Pedido implements Serializable {
    private long id;
    private Cliente cliente;
    private List<ItemPedido> itens;

    public Pedido(Cliente cliente) {
        this.cliente = cliente;
        this.itens = new ArrayList<>();
    }

    public Pedido(long id, Cliente cliente) {
        this.id = id;
        this.cliente = cliente;
        this.itens = new ArrayList<>();
    }

    public void adicionarItem(ItemPedido item) {
        itens.add(item);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public List<ItemPedido> getItens() {
        return itens;
    }

    public double getTotal() {
        // Usa Stream API para somar os subtotais de todos os itens
        return itens.stream().mapToDouble(ItemPedido::getSubtotal).sum();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pedido pedido = (Pedido) o;
        return id == pedido.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

// ==============================================================================
// 2. CLASSES DAO (Data Access Objects)
//    - Responsáveis por toda a interação com o banco de dados.
//    - Separa a lógica de persistência da lógica de negócio.
// ==============================================================================

/**
 * Interface genérica para um DAO.
 * Define as operações básicas de CRUD.
 */
interface DAO<T> {
    T salvar(T entity) throws SQLException;

    T buscarPorId(long id) throws SQLException;

    List<T> buscarTodos() throws SQLException;

    boolean atualizar(T entity) throws SQLException;

    boolean deletar(long id) throws SQLException;
}

/**
 * DAO para a entidade Cliente.
 * Gerencia a tabela 'clientes'.
 */
class ClienteDAO implements DAO<Cliente> {
    private Connection connection;

    public ClienteDAO(Connection connection) {
        this.connection = connection;
    }

    /**
     * Cria a tabela de clientes. Adicionada a instrução DROP para garantir
     * que a tabela seja sempre recriada com a estrutura correta.
     */
    public void criarTabela() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // **Aprimoramento:** Remove a tabela existente para evitar
            // erros de colunas ausentes em atualizações de versão.
            // **ATENÇÃO:** Isso apaga todos os dados existentes na tabela 'clientes'.
            stmt.execute("DROP TABLE IF EXISTS clientes");

            stmt.execute("CREATE TABLE IF NOT EXISTS clientes (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "nome TEXT NOT NULL," +
                    "email TEXT NOT NULL UNIQUE," +
                    "telefone TEXT," +
                    "endereco TEXT," +
                    "cidade TEXT," +
                    "estado TEXT," + // Campo 'estado'
                    "pais TEXT" +
                    ")");
        }
    }

    @Override
    public Cliente salvar(Cliente cliente) throws SQLException {
        // Verifica se já existe um cliente com o mesmo email
        if (buscarPorEmail(cliente.getEmail()) != null) {
            throw new SQLException("Já existe um cliente com o email: " + cliente.getEmail());
        }

        String sql = "INSERT INTO clientes (nome, email, telefone, endereco, cidade, estado, pais) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, cliente.getNome());
            pstmt.setString(2, cliente.getEmail());
            pstmt.setString(3, cliente.getTelefone());
            pstmt.setString(4, cliente.getEndereco());
            pstmt.setString(5, cliente.getCidade());
            pstmt.setString(6, cliente.getEstado());
            pstmt.setString(7, cliente.getPais());
            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    cliente.setId(rs.getLong(1)); // Define o ID gerado pelo banco
                }
            }
        }
        return cliente;
    }

    @Override
    public Cliente buscarPorId(long id) throws SQLException {
        String sql = "SELECT * FROM clientes WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    // Mapeia os dados do ResultSet para um objeto Cliente
                    return new Cliente(
                            rs.getLong("id"),
                            rs.getString("nome"),
                            rs.getString("email"),
                            rs.getString("telefone"),
                            rs.getString("endereco"),
                            rs.getString("cidade"),
                            rs.getString("estado"), // Campo 'estado'
                            rs.getString("pais")
                    );
                }
            }
        }
        return null;
    }

    public Cliente buscarPorEmail(String email) throws SQLException {
        String sql = "SELECT * FROM clientes WHERE email = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Cliente(
                            rs.getLong("id"),
                            rs.getString("nome"),
                            rs.getString("email"),
                            rs.getString("telefone"),
                            rs.getString("endereco"),
                            rs.getString("cidade"),
                            rs.getString("estado"), // Campo 'estado'
                            rs.getString("pais")
                    );
                }
            }
        }
        return null;
    }

    @Override
    public List<Cliente> buscarTodos() throws SQLException {
        List<Cliente> clientes = new ArrayList<>();
        String sql = "SELECT * FROM clientes";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                clientes.add(new Cliente(
                        rs.getLong("id"),
                        rs.getString("nome"),
                        rs.getString("email"),
                        rs.getString("telefone"),
                        rs.getString("endereco"),
                        rs.getString("cidade"),
                        rs.getString("estado"), // Campo 'estado'
                        rs.getString("pais")
                ));
            }
        }
        return clientes;
    }

    @Override
    public boolean atualizar(Cliente cliente) throws SQLException {
        String sql = "UPDATE clientes SET nome = ?, email = ?, telefone = ?, endereco = ?, cidade = ?, estado = ?, pais = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, cliente.getNome());
            pstmt.setString(2, cliente.getEmail());
            pstmt.setString(3, cliente.getTelefone());
            pstmt.setString(4, cliente.getEndereco());
            pstmt.setString(5, cliente.getCidade());
            pstmt.setString(6, cliente.getEstado());
            pstmt.setString(7, cliente.getPais());
            pstmt.setLong(8, cliente.getId());
            return pstmt.executeUpdate() > 0;
        }
    }

    @Override
    public boolean deletar(long id) throws SQLException {
        String sql = "DELETE FROM clientes WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }
}

/**
 * DAO para a entidade Produto.
 */
class ProdutoDAO implements DAO<Produto> {
    private Connection connection;

    public ProdutoDAO(Connection connection) {
        this.connection = connection;
    }

    public void criarTabela() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS produtos (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "nome TEXT NOT NULL UNIQUE," +
                    "preco REAL NOT NULL" +
                    ")");
        }
    }

    @Override
    public Produto salvar(Produto produto) throws SQLException {
        if (buscarPorNome(produto.getNome()) != null) {
            throw new SQLException("Já existe um produto com o nome: " + produto.getNome());
        }

        String sql = "INSERT INTO produtos (nome, preco) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, produto.getNome());
            pstmt.setDouble(2, produto.getPreco());
            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    produto.setId(rs.getLong(1));
                }
            }
        }
        return produto;
    }

    @Override
    public Produto buscarPorId(long id) throws SQLException {
        String sql = "SELECT id, nome, preco FROM produtos WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Produto(rs.getLong("id"), rs.getString("nome"), rs.getDouble("preco"));
                }
            }
        }
        return null;
    }

    public Produto buscarPorNome(String nome) throws SQLException {
        String sql = "SELECT id, nome, preco FROM produtos WHERE nome = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, nome);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Produto(rs.getLong("id"), rs.getString("nome"), rs.getDouble("preco"));
                }
            }
        }
        return null;
    }

    @Override
    public List<Produto> buscarTodos() throws SQLException {
        List<Produto> produtos = new ArrayList<>();
        String sql = "SELECT id, nome, preco FROM produtos";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                produtos.add(new Produto(rs.getLong("id"), rs.getString("nome"), rs.getDouble("preco")));
            }
        }
        return produtos;
    }

    @Override
    public boolean atualizar(Produto produto) throws SQLException {
        String sql = "UPDATE produtos SET nome = ?, preco = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, produto.getNome());
            pstmt.setDouble(2, produto.getPreco());
            pstmt.setLong(3, produto.getId());
            return pstmt.executeUpdate() > 0;
        }
    }

    @Override
    public boolean deletar(long id) throws SQLException {
        String sql = "DELETE FROM produtos WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }
}

/**
 * DAO para a entidade Pedido.
 */
class PedidoDAO implements DAO<Pedido> {
    private Connection connection;
    private ClienteDAO clienteDAO;
    private ProdutoDAO produtoDAO;

    public PedidoDAO(Connection connection, ClienteDAO clienteDAO, ProdutoDAO produtoDAO) {
        this.connection = connection;
        this.clienteDAO = clienteDAO;
        this.produtoDAO = produtoDAO;
    }

    /**
     * Cria as tabelas de pedidos e itens_pedido.
     */
    public void criarTabelas() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // Tabela de pedidos com chave estrangeira para clientes
            stmt.execute("CREATE TABLE IF NOT EXISTS pedidos (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "cliente_id INTEGER NOT NULL," +
                    "FOREIGN KEY (cliente_id) REFERENCES clientes(id) ON DELETE CASCADE" +
                    ")");
            // Tabela de itens_pedido com chaves estrangeiras para pedidos e produtos
            stmt.execute("CREATE TABLE IF NOT EXISTS itens_pedido (" +
                    "pedido_id INTEGER NOT NULL," +
                    "produto_id INTEGER NOT NULL," +
                    "quantidade INTEGER NOT NULL," +
                    "PRIMARY KEY (pedido_id, produto_id)," +
                    "FOREIGN KEY (pedido_id) REFERENCES pedidos(id) ON DELETE CASCADE," +
                    "FOREIGN KEY (produto_id) REFERENCES produtos(id)" +
                    ")");
        }
    }

    @Override
    public Pedido salvar(Pedido pedido) throws SQLException {
        // Salva o pedido principal e obtém o ID
        String sqlPedido = "INSERT INTO pedidos (cliente_id) VALUES (?)";
        try (PreparedStatement pstmtPedido = connection.prepareStatement(sqlPedido, Statement.RETURN_GENERATED_KEYS)) {
            pstmtPedido.setLong(1, pedido.getCliente().getId());
            pstmtPedido.executeUpdate();
            try (ResultSet rs = pstmtPedido.getGeneratedKeys()) {
                if (rs.next()) {
                    pedido.setId(rs.getLong(1));
                }
            }
        }

        // Salva cada item do pedido
        String sqlItem = "INSERT INTO itens_pedido (pedido_id, produto_id, quantidade) VALUES (?, ?, ?)";
        try (PreparedStatement pstmtItem = connection.prepareStatement(sqlItem)) {
            for (ItemPedido item : pedido.getItens()) {
                pstmtItem.setLong(1, pedido.getId());
                pstmtItem.setLong(2, item.getProduto().getId());
                pstmtItem.setInt(3, item.getQuantidade());
                pstmtItem.addBatch(); // Adiciona ao lote para execução em massa
            }
            pstmtItem.executeBatch(); // Executa todos os inserts de uma vez
        }
        return pedido;
    }

    @Override
    public Pedido buscarPorId(long id) throws SQLException {
        // Busca o pedido principal
        String sqlPedido = "SELECT id, cliente_id FROM pedidos WHERE id = ?";
        Pedido pedido = null;
        try (PreparedStatement pstmtPedido = connection.prepareStatement(sqlPedido)) {
            pstmtPedido.setLong(1, id);
            try (ResultSet rsPedido = pstmtPedido.executeQuery()) {
                if (rsPedido.next()) {
                    long clienteId = rsPedido.getLong("cliente_id");
                    Cliente cliente = clienteDAO.buscarPorId(clienteId);
                    if (cliente != null) {
                        pedido = new Pedido(rsPedido.getLong("id"), cliente);
                    }
                }
            }
        }

        // Busca os itens se o pedido foi encontrado
        if (pedido != null) {
            String sqlItens = "SELECT produto_id, quantidade FROM itens_pedido WHERE pedido_id = ?";
            try (PreparedStatement pstmtItens = connection.prepareStatement(sqlItens)) {
                pstmtItens.setLong(1, pedido.getId());
                try (ResultSet rsItens = pstmtItens.executeQuery()) {
                    while (rsItens.next()) {
                        long produtoId = rsItens.getLong("produto_id");
                        int quantidade = rsItens.getInt("quantidade");
                        Produto produto = produtoDAO.buscarPorId(produtoId);
                        if (produto != null) {
                            pedido.adicionarItem(new ItemPedido(produto, quantidade));
                        }
                    }
                }
            }
        }
        return pedido;
    }

    @Override
    public List<Pedido> buscarTodos() throws SQLException {
        List<Pedido> pedidos = new ArrayList<>();
        String sql = "SELECT id, cliente_id FROM pedidos";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                // Para cada pedido, busca o cliente e os itens associados
                long clienteId = rs.getLong("cliente_id");
                Cliente cliente = clienteDAO.buscarPorId(clienteId);
                if (cliente != null) {
                    Pedido pedido = new Pedido(rs.getLong("id"), cliente);
                    String sqlItens = "SELECT produto_id, quantidade FROM itens_pedido WHERE pedido_id = ?";
                    try (PreparedStatement pstmtItens = connection.prepareStatement(sqlItens)) {
                        pstmtItens.setLong(1, pedido.getId());
                        try (ResultSet rsItens = pstmtItens.executeQuery()) {
                            while (rsItens.next()) {
                                long produtoId = rsItens.getLong("produto_id");
                                int quantidade = rsItens.getInt("quantidade");
                                Produto produto = produtoDAO.buscarPorId(produtoId);
                                if (produto != null) {
                                    pedido.adicionarItem(new ItemPedido(produto, quantidade));
                                }
                            }
                        }
                    }
                    pedidos.add(pedido);
                }
            }
        }
        return pedidos;
    }

    @Override
    public boolean atualizar(Pedido pedido) throws SQLException {
        // Atualiza os dados do pedido principal
        String sqlPedido = "UPDATE pedidos SET cliente_id = ? WHERE id = ?";
        try (PreparedStatement pstmtPedido = connection.prepareStatement(sqlPedido)) {
            pstmtPedido.setLong(1, pedido.getCliente().getId());
            pstmtPedido.setLong(2, pedido.getId());
            pstmtPedido.executeUpdate();
        }

        // Deleta todos os itens antigos e insere os novos
        String sqlDeleteItens = "DELETE FROM itens_pedido WHERE pedido_id = ?";
        try (PreparedStatement pstmtDelete = connection.prepareStatement(sqlDeleteItens)) {
            pstmtDelete.setLong(1, pedido.getId());
            pstmtDelete.executeUpdate();
        }

        String sqlInsertItem = "INSERT INTO itens_pedido (pedido_id, produto_id, quantidade) VALUES (?, ?, ?)";
        try (PreparedStatement pstmtInsert = connection.prepareStatement(sqlInsertItem)) {
            for (ItemPedido item : pedido.getItens()) {
                pstmtInsert.setLong(1, pedido.getId());
                pstmtInsert.setLong(2, item.getProduto().getId());
                pstmtInsert.setInt(3, item.getQuantidade());
                pstmtInsert.addBatch();
            }
            pstmtInsert.executeBatch();
        }
        return true;
    }

    @Override
    public boolean deletar(long id) throws SQLException {
        String sql = "DELETE FROM pedidos WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }
}

// ==============================================================================
// 3. CLASSE DO CONTROLADOR (ERPController)
//    - Lida com a lógica de negócio, conectando a View e o Model/DAO.
// ==============================================================================

/**
 * Classe principal do controlador.
 * Gerencia as operações de alto nível da aplicação e as DAOs.
 */
class ERPController {
    private Connection connection;
    private ClienteDAO clienteDAO;
    private ProdutoDAO produtoDAO;
    private PedidoDAO pedidoDAO;

    public ERPController() {
        try {
            // Carrega o driver JDBC para SQLite
            Class.forName("org.sqlite.JDBC");
            // Conecta ao banco de dados (o arquivo será criado se não existir)
            connection = DriverManager.getConnection("jdbc:sqlite:erp_database.db");
            // Habilita chaves estrangeiras no SQLite
            connection.createStatement().execute("PRAGMA foreign_keys = ON");

            // Instancia as DAOs
            clienteDAO = new ClienteDAO(connection);
            produtoDAO = new ProdutoDAO(connection);
            pedidoDAO = new PedidoDAO(connection, clienteDAO, produtoDAO);

            // Cria as tabelas se não existirem
            clienteDAO.criarTabela();
            produtoDAO.criarTabela();
            pedidoDAO.criarTabelas();

        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Erro ao inicializar o banco de dados: " + e.getMessage(), "Erro de Banco de Dados", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    // Métodos de negócio para Clientes
    public Cliente adicionarCliente(Cliente c) throws SQLException {
        return clienteDAO.salvar(c);
    }

    public boolean atualizarCliente(Cliente c) throws SQLException {
        return clienteDAO.atualizar(c);
    }

    public boolean removerCliente(long id) throws SQLException {
        // Lógica de negócio: impede a exclusão de um cliente com pedidos associados
        boolean clienteTemPedidos = pedidoDAO.buscarTodos().stream()
                .anyMatch(p -> p.getCliente().getId() == id);
        if (clienteTemPedidos) {
            return false;
        }
        return clienteDAO.deletar(id);
    }

    public List<Cliente> getClientes() throws SQLException {
        return clienteDAO.buscarTodos();
    }

    // Métodos de negócio para Produtos
    public Produto adicionarProduto(Produto p) throws SQLException {
        return produtoDAO.salvar(p);
    }

    public boolean atualizarProduto(Produto p) throws SQLException {
        return produtoDAO.atualizar(p);
    }

    public boolean removerProduto(long id) throws SQLException {
        // Lógica de negócio: impede a exclusão de um produto usado em pedidos
        boolean produtoUsadoEmPedido = pedidoDAO.buscarTodos().stream()
                .anyMatch(p -> p.getItens().stream()
                        .anyMatch(item -> item.getProduto().getId() == id));
        if (produtoUsadoEmPedido) {
            return false;
        }
        return produtoDAO.deletar(id);
    }

    public List<Produto> getProdutos() throws SQLException {
        return produtoDAO.buscarTodos();
    }

    // Métodos de negócio para Pedidos
    public Pedido adicionarPedido(Pedido p) throws SQLException {
        return pedidoDAO.salvar(p);
    }

    public boolean atualizarPedido(Pedido p) throws SQLException {
        return pedidoDAO.atualizar(p);
    }

    public boolean removerPedido(long id) throws SQLException {
        return pedidoDAO.deletar(id);
    }

    public List<Pedido> getPedidos() throws SQLException {
        return pedidoDAO.buscarTodos();
    }

    // Método para exportar o relatório para um arquivo CSV
    public void exportarRelatorioCSV(File arquivo) throws IOException, SQLException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(arquivo))) {
            writer.write("Cliente;Produto;Quantidade;Subtotal\n");

            for (Pedido p : pedidoDAO.buscarTodos()) {
                for (ItemPedido item : p.getItens()) {
                    writer.write(String.format("%s;%s;%d;%.2f\n",
                            p.getCliente().getNome(),
                            item.getProduto().getNome(),
                            item.getQuantidade(),
                            item.getSubtotal()));
                }
            }
        }
    }

    // Fecha a conexão com o banco de dados
    public void fecharConexao() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}


// ==============================================================================
// 4. CLASSES DA VISÃO (Painéis da Interface Gráfica)
//    - Representam a interface de usuário (View) e interagem com o Controller.
// ==============================================================================

/**
 * Utilitário para formatar strings.
 */
class StringUtils {
    public static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}


/**
 * Painel da interface para gerenciar Clientes.
 * Inclui campos de texto, botões e uma tabela.
 */
class ClientesPanel extends JPanel {
    private ERPController controller;
    private DefaultTableModel modeloClientes;
    private JTable tabela;
    private JTextField txtNome, txtEmail, txtTelefone, txtEndereco, txtCidade, txtEstado, txtPais;
    private JButton btnSalvar, btnExcluir, btnEditar;

    public ClientesPanel(ERPController controller) {
        this.controller = controller;
        setLayout(new BorderLayout());

        // Define o modelo da tabela com as colunas do cliente
        modeloClientes = new DefaultTableModel(new Object[]{"ID", "Nome", "Email", "Telefone", "Endereço", "Cidade", "Estado", "País"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Torna as células da tabela não editáveis
            }
        };
        tabela = new JTable(modeloClientes);
        tabela.setAutoCreateRowSorter(true); // Permite ordenar a tabela
        JScrollPane scrollPane = new JScrollPane(tabela);
        add(scrollPane, BorderLayout.CENTER);

        // Listener para carregar os dados sempre que o painel for exibido
        this.addAncestorListener(new AncestorListener() {
            public void ancestorAdded(AncestorEvent evt) {
                carregarClientesNaTabela();
            }

            public void ancestorRemoved(AncestorEvent evt) {
            }

            public void ancestorMoved(AncestorEvent evt) {
            }
        });

        // Configuração do formulário de entrada
        JPanel form = new JPanel(new GridLayout(0, 2, 5, 5));
        txtNome = new JTextField(15);
        txtEmail = new JTextField(15);
        txtTelefone = new JTextField(15);
        txtEndereco = new JTextField(15);
        txtCidade = new JTextField(15);
        txtEstado = new JTextField(15);
        txtPais = new JTextField(15);

        form.add(new JLabel("Nome*:"));
        form.add(txtNome);
        form.add(new JLabel("Email*:"));
        form.add(txtEmail);
        form.add(new JLabel("Telefone*:"));
        form.add(txtTelefone);
        form.add(new JLabel("Endereço:"));
        form.add(txtEndereco);
        form.add(new JLabel("Cidade:"));
        form.add(txtCidade);
        form.add(new JLabel("Estado:"));
        form.add(txtEstado);
        form.add(new JLabel("País:"));
        form.add(txtPais);

        // Configuração dos botões
        JPanel botoes = new JPanel();
        btnSalvar = new JButton("Cadastrar");
        btnEditar = new JButton("Editar Selecionado");
        btnExcluir = new JButton("Excluir Selecionado");

        botoes.add(btnSalvar);
        botoes.add(btnEditar);
        botoes.add(btnExcluir);

        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(form, BorderLayout.CENTER);
        southPanel.add(botoes, BorderLayout.SOUTH);

        add(southPanel, BorderLayout.SOUTH);

        // Adiciona os listeners para os botões
        btnSalvar.addActionListener(e -> cadastrarOuAtualizarCliente(false));
        btnEditar.addActionListener(e -> carregarClienteParaEdicao());
        btnExcluir.addActionListener(e -> excluirCliente());
    }

    /**
     * Carrega os dados dos clientes do banco e exibe na tabela.
     */
    private void carregarClientesNaTabela() {
        modeloClientes.setRowCount(0); // Limpa a tabela
        try {
            for (Cliente c : controller.getClientes()) {
                modeloClientes.addRow(new Object[]{
                        c.getId(),
                        c.getNome(),
                        c.getEmail(),
                        c.getTelefone(),
                        c.getEndereco(),
                        c.getCidade(),
                        c.getEstado(), // Campo 'estado'
                        c.getPais()
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, StringUtils.capitalize("Erro ao carregar clientes: " + ex.getMessage()), StringUtils.capitalize("Erro de BD"), JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    /**
     * Lógica para cadastrar ou atualizar um cliente.
     */
    private void cadastrarOuAtualizarCliente(boolean isUpdate) {
        // Coleta os dados dos campos de texto
        String nome = txtNome.getText().trim();
        String email = txtEmail.getText().trim();
        String telefone = txtTelefone.getText().trim();
        String endereco = txtEndereco.getText().trim();
        String cidade = txtCidade.getText().trim();
        String estado = txtEstado.getText().trim();
        String pais = txtPais.getText().trim();

        // Validação básica: nome, email e telefone são obrigatórios
        if (nome.isEmpty() || email.isEmpty() || telefone.isEmpty()) {
            JOptionPane.showMessageDialog(this, StringUtils.capitalize("nome, email e telefone são obrigatórios."));
            return;
        }

        try {
            if (isUpdate) {
                // Se for uma atualização, obtém o ID da linha selecionada
                int row = tabela.getSelectedRow();
                if (row == -1) {
                    JOptionPane.showMessageDialog(this, StringUtils.capitalize("selecione um cliente para editar"));
                    btnSalvar.setText("Cadastrar");
                    return;
                }
                long id = (long) modeloClientes.getValueAt(tabela.convertRowIndexToModel(row), 0);
                Cliente c = new Cliente(id, nome, email, telefone, endereco, cidade, estado, pais);
                if (controller.atualizarCliente(c)) {
                    JOptionPane.showMessageDialog(this, StringUtils.capitalize("cliente atualizado com sucesso!"));
                } else {
                    JOptionPane.showMessageDialog(this, StringUtils.capitalize("erro ao atualizar cliente."), StringUtils.capitalize("Erro"), JOptionPane.ERROR_MESSAGE);
                }
            } else {
                // Se for um novo cadastro
                Cliente c = new Cliente(nome, email, telefone, endereco, cidade, estado, pais);
                controller.adicionarCliente(c);
                JOptionPane.showMessageDialog(this, StringUtils.capitalize("cliente cadastrado com sucesso"));
            }

            // Limpa os campos após a operação
            txtNome.setText("");
            txtEmail.setText("");
            txtTelefone.setText("");
            txtEndereco.setText("");
            txtCidade.setText("");
            txtEstado.setText("");
            txtPais.setText("");
            btnSalvar.setText("Cadastrar");
            carregarClientesNaTabela();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, StringUtils.capitalize("erro ao salvar/atualizar cliente: " + ex.getMessage()), StringUtils.capitalize("Erro de BD"), JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    /**
     * Carrega os dados do cliente selecionado para edição.
     */
    private void carregarClienteParaEdicao() {
        int row = tabela.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, StringUtils.capitalize("selecione um cliente para editar"));
            return;
        }

        int modelRow = tabela.convertRowIndexToModel(row);
        txtNome.setText((String) modeloClientes.getValueAt(modelRow, 1));
        txtEmail.setText((String) modeloClientes.getValueAt(modelRow, 2));
        txtTelefone.setText((String) modeloClientes.getValueAt(modelRow, 3));
        txtEndereco.setText((String) modeloClientes.getValueAt(modelRow, 4));
        txtCidade.setText((String) modeloClientes.getValueAt(modelRow, 5));
        txtEstado.setText((String) modeloClientes.getValueAt(modelRow, 6));
        txtPais.setText((String) modeloClientes.getValueAt(modelRow, 7));

        btnSalvar.setText("Atualizar");

        // Remove o listener de cadastro e adiciona o de atualização para o botão Salvar
        for (ActionListener al : btnSalvar.getActionListeners()) {
            btnSalvar.removeActionListener(al);
        }
        btnSalvar.addActionListener(e -> cadastrarOuAtualizarCliente(true));
    }

    /**
     * Lógica para excluir um cliente.
     */
    private void excluirCliente() {
        int row = tabela.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, StringUtils.capitalize("selecione um cliente para excluir"));
            return;
        }

        int modelRow = tabela.convertRowIndexToModel(row);
        long id = (long) modeloClientes.getValueAt(modelRow, 0);
        String nome = (String) modeloClientes.getValueAt(modelRow, 1);

        int confirm = JOptionPane.showConfirmDialog(this, StringUtils.capitalize("tem certeza que deseja excluir o cliente " + nome + "?\n(Esta ação pode falhar se houver pedidos associados)"), StringUtils.capitalize("confirmar exclusão"), JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                if (controller.removerCliente(id)) {
                    JOptionPane.showMessageDialog(this, StringUtils.capitalize("cliente excluído com sucesso!"));
                    carregarClientesNaTabela();
                } else {
                    JOptionPane.showMessageDialog(this, StringUtils.capitalize("não foi possível excluir o cliente. ele está associado a um ou mais pedidos."), StringUtils.capitalize("Erro de exclusão"), JOptionPane.ERROR_MESSAGE);
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, StringUtils.capitalize("erro ao excluir cliente: " + ex.getMessage()), StringUtils.capitalize("Erro de BD"), JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }
}

/**
 * Painel da interface para gerenciar Produtos.
 */
class ProdutosPanel extends JPanel {
    private ERPController controller;
    private DefaultTableModel modeloProdutos;
    private JTable tabela;
    private JTextField txtNome, txtPreco;
    private JButton btnSalvar, btnExcluir, btnEditar;

    public ProdutosPanel(ERPController controller) {
        this.controller = controller;
        setLayout(new BorderLayout());

        modeloProdutos = new DefaultTableModel(new Object[]{"ID", "Nome", "Preço"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tabela = new JTable(modeloProdutos);
        tabela.setAutoCreateRowSorter(true);
        JScrollPane scrollPane = new JScrollPane(tabela);
        add(scrollPane, BorderLayout.CENTER);

        this.addAncestorListener(new AncestorListener() {
            public void ancestorAdded(AncestorEvent evt) {
                carregarProdutosNaTabela();
            }

            public void ancestorRemoved(AncestorEvent evt) {
            }

            public void ancestorMoved(AncestorEvent evt) {
            }
        });

        JPanel form = new JPanel();
        txtNome = new JTextField(10);
        txtPreco = new JTextField(5);
        btnSalvar = new JButton("Cadastrar");
        btnEditar = new JButton("Editar Selecionado");
        btnExcluir = new JButton("Excluir Selecionado");

        form.add(new JLabel("Nome:"));
        form.add(txtNome);
        form.add(new JLabel("Preço:"));
        form.add(txtPreco);
        form.add(btnSalvar);
        form.add(btnEditar);
        form.add(btnExcluir);

        add(form, BorderLayout.SOUTH);

        btnSalvar.addActionListener(e -> cadastrarOuAtualizarProduto(false));
        btnEditar.addActionListener(e -> carregarProdutoParaEdicao());
        btnExcluir.addActionListener(e -> excluirProduto());
    }

    private void carregarProdutosNaTabela() {
        modeloProdutos.setRowCount(0);
        try {
            for (Produto p : controller.getProdutos()) {
                modeloProdutos.addRow(new Object[]{p.getId(), p.getNome(), p.getPreco()});
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, StringUtils.capitalize("erro ao carregar produtos: " + ex.getMessage()), StringUtils.capitalize("Erro de BD"), JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private void cadastrarOuAtualizarProduto(boolean isUpdate) {
        String nome = txtNome.getText().trim();
        String precoStr = txtPreco.getText().trim();
        if (nome.isEmpty() || precoStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, StringUtils.capitalize("preencha todos os campos"));
            return;
        }

        try {
            double preco = Double.parseDouble(precoStr);
            if (preco <= 0) {
                JOptionPane.showMessageDialog(this, StringUtils.capitalize("o preço deve ser maior que zero."));
                return;
            }

            if (isUpdate) {
                int row = tabela.getSelectedRow();
                if (row == -1) {
                    JOptionPane.showMessageDialog(this, StringUtils.capitalize("selecione um produto para editar"));
                    btnSalvar.setText("Cadastrar");
                    return;
                }
                long id = (long) modeloProdutos.getValueAt(tabela.convertRowIndexToModel(row), 0);
                Produto p = new Produto(id, nome, preco);
                if (controller.atualizarProduto(p)) {
                    JOptionPane.showMessageDialog(this, StringUtils.capitalize("produto atualizado com sucesso!"));
                } else {
                    JOptionPane.showMessageDialog(this, StringUtils.capitalize("erro ao atualizar produto."), StringUtils.capitalize("Erro"), JOptionPane.ERROR_MESSAGE);
                }
            } else {
                Produto p = new Produto(nome, preco);
                controller.adicionarProduto(p);
                JOptionPane.showMessageDialog(this, StringUtils.capitalize("produto cadastrado com sucesso"));
            }
            txtNome.setText("");
            txtPreco.setText("");
            btnSalvar.setText("Cadastrar");
            carregarProdutosNaTabela();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, StringUtils.capitalize("preço inválido. use números."));
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, StringUtils.capitalize("erro ao salvar/atualizar produto: " + ex.getMessage()), StringUtils.capitalize("Erro de BD"), JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private void carregarProdutoParaEdicao() {
        int row = tabela.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, StringUtils.capitalize("selecione um produto para editar"));
            return;
        }

        int modelRow = tabela.convertRowIndexToModel(row);
        txtNome.setText((String) modeloProdutos.getValueAt(modelRow, 1));
        txtPreco.setText(String.valueOf(modeloProdutos.getValueAt(modelRow, 2)));
        btnSalvar.setText("Atualizar");

        for (ActionListener al : btnSalvar.getActionListeners()) {
            btnSalvar.removeActionListener(al);
        }
        btnSalvar.addActionListener(e -> cadastrarOuAtualizarProduto(true));
    }

    private void excluirProduto() {
        int row = tabela.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, StringUtils.capitalize("selecione um produto para excluir"));
            return;
        }

        int modelRow = tabela.convertRowIndexToModel(row);
        long id = (long) modeloProdutos.getValueAt(modelRow, 0);
        String nome = (String) modeloProdutos.getValueAt(modelRow, 1);

        int confirm = JOptionPane.showConfirmDialog(this, StringUtils.capitalize("tem certeza que deseja excluir o produto " + nome + "?"), StringUtils.capitalize("Confirmar exclusão"), JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                if (controller.removerProduto(id)) {
                    JOptionPane.showMessageDialog(this, StringUtils.capitalize("produto excluído com sucesso!"));
                    carregarProdutosNaTabela();
                } else {
                    JOptionPane.showMessageDialog(this, StringUtils.capitalize("não foi possível excluir o produto. ele está associado a um ou mais pedidos."), StringUtils.capitalize("Erro de exclusão"), JOptionPane.ERROR_MESSAGE);
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, StringUtils.capitalize("erro ao excluir produto: " + ex.getMessage()), StringUtils.capitalize("Erro de BD"), JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }
}

/**
 * Painel da interface para gerenciar Pedidos.
 */
class PedidosPanel extends JPanel {
    private ERPController controller;
    private DefaultTableModel modeloPedidos;
    private JTable tabela;
    private JButton btnNovo, btnExcluir, btnEditar;
    private JFrame parentFrame;

    public PedidosPanel(ERPController controller, JFrame parentFrame) {
        this.controller = controller;
        this.parentFrame = parentFrame;
        setLayout(new BorderLayout());

        modeloPedidos = new DefaultTableModel(new Object[]{"ID", "Cliente", "Total"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tabela = new JTable(modeloPedidos);
        tabela.setAutoCreateRowSorter(true);
        JScrollPane scrollPane = new JScrollPane(tabela);
        add(scrollPane, BorderLayout.CENTER);

        this.addAncestorListener(new AncestorListener() {
            public void ancestorAdded(AncestorEvent evt) {
                carregarPedidosNaTabela();
            }

            public void ancestorRemoved(AncestorEvent evt) {
            }

            public void ancestorMoved(AncestorEvent evt) {
            }
        });

        JPanel botoes = new JPanel();
        btnNovo = new JButton("Novo Pedido");
        btnEditar = new JButton("Editar Selecionado");
        btnExcluir = new JButton("Excluir Selecionado");

        botoes.add(btnNovo);
        botoes.add(btnEditar);
        botoes.add(btnExcluir);

        add(botoes, BorderLayout.SOUTH);

        btnNovo.addActionListener(e -> abrirDialogoNovoPedido(null));
        btnEditar.addActionListener(e -> editarPedidoSelecionado());
        btnExcluir.addActionListener(e -> excluirPedido());
    }

    private void carregarPedidosNaTabela() {
        modeloPedidos.setRowCount(0);
        try {
            for (Pedido p : controller.getPedidos()) {
                modeloPedidos.addRow(new Object[]{p.getId(), p.getCliente().getNome(), String.format("%.2f", p.getTotal())});
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, StringUtils.capitalize("erro ao carregar pedidos: " + ex.getMessage()), StringUtils.capitalize("Erro de BD"), JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    /**
     * Abre um diálogo para criar ou editar um pedido.
     */
    private void abrirDialogoNovoPedido(Pedido pedidoParaEditar) {
        try {
            if (controller.getClientes().isEmpty() || controller.getProdutos().isEmpty()) {
                JOptionPane.showMessageDialog(this, StringUtils.capitalize("cadastre clientes e produtos antes de criar pedidos"));
                return;
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, StringUtils.capitalize("erro ao verificar clientes/produtos: " + ex.getMessage()), StringUtils.capitalize("Erro de BD"), JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
            return;
        }

        JDialog dialogo = new JDialog(parentFrame, (pedidoParaEditar == null ? "Novo Pedido" : "Editar Pedido"), true);
        dialogo.setSize(700, 400);
        dialogo.setLayout(new BorderLayout());

        // Componentes do formulário do pedido
        JComboBox<Cliente> comboClientes = new JComboBox<>();
        try {
            for (Cliente c : controller.getClientes()) comboClientes.addItem(c);
        } catch (SQLException ex) { /* Tratamento de erro */ }

        JComboBox<Produto> comboProdutos = new JComboBox<>();
        try {
            for (Produto p : controller.getProdutos()) comboProdutos.addItem(p);
        } catch (SQLException ex) { /* Tratamento de erro */ }

        JTextField txtQtd = new JTextField(3);
        DefaultTableModel modeloItens = new DefaultTableModel(new Object[]{"ID Produto", "Produto", "Qtd", "Subtotal"}, 0);
        JTable tabelaItens = new JTable(modeloItens);
        tabelaItens.setAutoCreateRowSorter(true);

        JButton btnAdicionar = new JButton("Adicionar Item");
        JButton btnSalvar = new JButton("Salvar Pedido");

        JPanel form = new JPanel();
        form.add(new JLabel("Cliente:"));
        form.add(comboClientes);
        form.add(new JLabel("Produto:"));
        form.add(comboProdutos);
        form.add(new JLabel("Qtd:"));
        form.add(txtQtd);
        form.add(btnAdicionar);

        dialogo.add(form, BorderLayout.NORTH);
        dialogo.add(new JScrollPane(tabelaItens), BorderLayout.CENTER);
        dialogo.add(btnSalvar, BorderLayout.SOUTH);

        List<ItemPedido> itensTemp = new ArrayList<>();

        // Se for uma edição, preenche o diálogo com os dados do pedido existente
        if (pedidoParaEditar != null) {
            comboClientes.setSelectedItem(pedidoParaEditar.getCliente());
            comboClientes.setEnabled(false); // Impede a alteração do cliente em um pedido existente
            itensTemp.addAll(pedidoParaEditar.getItens());
            for (ItemPedido item : pedidoParaEditar.getItens()) {
                modeloItens.addRow(new Object[]{item.getProduto().getId(), item.getProduto().getNome(), item.getQuantidade(), item.getSubtotal()});
            }
        }

        // Lógica para adicionar um item à lista temporária
        btnAdicionar.addActionListener(ev -> {
            try {
                Produto prod = (Produto) comboProdutos.getSelectedItem();
                if (prod == null) {
                    JOptionPane.showMessageDialog(dialogo, StringUtils.capitalize("selecione um produto."));
                    return;
                }
                int qtd = Integer.parseInt(txtQtd.getText());
                if (qtd <= 0) {
                    JOptionPane.showMessageDialog(dialogo, StringUtils.capitalize("quantidade deve ser maior que zero"));
                    return;
                }

                boolean found = false;
                for (ItemPedido existingItem : itensTemp) {
                    if (existingItem.getProduto().equals(prod)) {
                        // Se o produto já existe, atualiza a quantidade (lógica de idempotência)
                        existingItem = new ItemPedido(prod, existingItem.getQuantidade() + qtd);
                        itensTemp.set(itensTemp.indexOf(existingItem), existingItem);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    itensTemp.add(new ItemPedido(prod, qtd));
                }

                modeloItens.setRowCount(0);
                for (ItemPedido item : itensTemp) {
                    modeloItens.addRow(new Object[]{item.getProduto().getId(), item.getProduto().getNome(), item.getQuantidade(), item.getSubtotal()});
                }
                txtQtd.setText("");

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialogo, StringUtils.capitalize("quantidade inválida"));
            }
        });

        // Lógica para salvar o pedido
        btnSalvar.addActionListener(ev -> {
            if (itensTemp.isEmpty()) {
                JOptionPane.showMessageDialog(dialogo, StringUtils.capitalize("o pedido deve conter pelo menos um item."));
                return;
            }

            Cliente cliente = (Cliente) comboClientes.getSelectedItem();
            if (cliente == null) {
                JOptionPane.showMessageDialog(dialogo, StringUtils.capitalize("selecione um cliente."));
                return;
            }

            try {
                if (pedidoParaEditar == null) {
                    Pedido pedido = new Pedido(cliente);
                    for (ItemPedido item : itensTemp) pedido.adicionarItem(item);
                    controller.adicionarPedido(pedido);
                    JOptionPane.showMessageDialog(dialogo, StringUtils.capitalize(String.format("pedido salvo com sucesso! total: R$ %.2f", pedido.getTotal())));
                } else {
                    pedidoParaEditar.getItens().clear(); // Limpa os itens antigos e adiciona os novos
                    for (ItemPedido item : itensTemp) pedidoParaEditar.adicionarItem(item);
                    controller.atualizarPedido(pedidoParaEditar);
                    JOptionPane.showMessageDialog(dialogo, StringUtils.capitalize(String.format("pedido atualizado com sucesso! total: R$ %.2f", pedidoParaEditar.getTotal())));
                }
                carregarPedidosNaTabela();
                dialogo.dispose();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(dialogo, StringUtils.capitalize("erro ao salvar/atualizar pedido: " + ex.getMessage()), StringUtils.capitalize("Erro de BD"), JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        });

        dialogo.setLocationRelativeTo(this);
        dialogo.setVisible(true);
    }

    /**
     * Carrega os dados do pedido selecionado para o diálogo de edição.
     */
    private void editarPedidoSelecionado() {
        int row = tabela.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, StringUtils.capitalize("selecione um pedido para editar"));
            return;
        }

        int modelRow = tabela.convertRowIndexToModel(row);
        long pedidoId = (long) modeloPedidos.getValueAt(modelRow, 0);

        try {
            Pedido pedido = null;
            // Busca o pedido completo na lista do controller
            for (Pedido p : controller.getPedidos()) {
                if (p.getId() == pedidoId) {
                    pedido = p;
                    break;
                }
            }
            if (pedido != null) {
                abrirDialogoNovoPedido(pedido);
            } else {
                JOptionPane.showMessageDialog(this, StringUtils.capitalize("pedido não encontrado no sistema."), StringUtils.capitalize("Erro"), JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, StringUtils.capitalize("erro ao carregar pedido para edição: " + ex.getMessage()), StringUtils.capitalize("Erro de BD"), JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    /**
     * Lógica para excluir um pedido.
     */
    private void excluirPedido() {
        int row = tabela.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, StringUtils.capitalize("selecione um pedido para excluir"));
            return;
        }

        int modelRow = tabela.convertRowIndexToModel(row);
        long id = (long) modeloPedidos.getValueAt(modelRow, 0);

        int confirm = JOptionPane.showConfirmDialog(this, StringUtils.capitalize("tem certeza que deseja excluir este pedido? (esta ação é irreversível)"), StringUtils.capitalize("Confirmar exclusão"), JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                if (controller.removerPedido(id)) {
                    JOptionPane.showMessageDialog(this, StringUtils.capitalize("pedido excluído com sucesso!"));
                    carregarPedidosNaTabela();
                } else {
                    JOptionPane.showMessageDialog(this, StringUtils.capitalize("erro ao excluir o pedido."), StringUtils.capitalize("Erro"), JOptionPane.ERROR_MESSAGE);
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, StringUtils.capitalize("erro ao excluir pedido: " + ex.getMessage()), StringUtils.capitalize("Erro de BD"), JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }
}

/**
 * Painel para exibir o relatório de pedidos e exportar para CSV.
 */
class RelatorioPanel extends JPanel {
    private ERPController controller;
    private DefaultTableModel modeloRelatorio;
    private JTable tabelaRelatorio;
    private JButton btnGerar, btnExportarCSV;

    public RelatorioPanel(ERPController controller) {
        this.controller = controller;
        setLayout(new BorderLayout());

        modeloRelatorio = new DefaultTableModel(new Object[]{"Cliente", "Produto", "Qtd", "Subtotal"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tabelaRelatorio = new JTable(modeloRelatorio);
        tabelaRelatorio.setAutoCreateRowSorter(true);
        add(new JScrollPane(tabelaRelatorio), BorderLayout.CENTER);

        // Gera o relatório automaticamente ao abrir a aba
        this.addAncestorListener(new AncestorListener() {
            public void ancestorAdded(AncestorEvent evt) {
                gerarRelatorio();
            }

            public void ancestorRemoved(AncestorEvent evt) {
            }

            public void ancestorMoved(AncestorEvent evt) {
            }
        });

        JPanel botoesPainel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnGerar = new JButton("Atualizar Relatório");
        btnExportarCSV = new JButton("Exportar para CSV");

        botoesPainel.add(btnGerar);
        botoesPainel.add(btnExportarCSV);

        add(botoesPainel, BorderLayout.SOUTH);

        btnGerar.addActionListener(e -> gerarRelatorio());
        btnExportarCSV.addActionListener(e -> exportarCSV());
    }

    /**
     * Gera e exibe o relatório na tabela.
     */
    private void gerarRelatorio() {
        modeloRelatorio.setRowCount(0);
        double totalGeral = 0;
        try {
            // Itera sobre todos os pedidos e seus itens para popular a tabela
            for (Pedido p : controller.getPedidos()) {
                for (ItemPedido item : p.getItens()) {
                    modeloRelatorio.addRow(new Object[]{p.getCliente().getNome(), item.getProduto().getNome(), item.getQuantidade(), item.getSubtotal()});
                    totalGeral += item.getSubtotal();
                }
            }
            JOptionPane.showMessageDialog(this, StringUtils.capitalize(String.format("total geral de todos os pedidos: R$ %.2f", totalGeral)));
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, StringUtils.capitalize("erro ao gerar relatório: " + ex.getMessage()), StringUtils.capitalize("Erro de BD"), JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    /**
     * Exporta o relatório para um arquivo CSV.
     */
    private void exportarCSV() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Salvar Relatório CSV");
        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            if (!fileToSave.getAbsolutePath().endsWith(".csv")) {
                fileToSave = new File(fileToSave.getAbsolutePath() + ".csv");
            }
            try {
                controller.exportarRelatorioCSV(fileToSave);
                JOptionPane.showMessageDialog(this, StringUtils.capitalize("relatório exportado com sucesso para:\n" + fileToSave.getAbsolutePath()));
            } catch (IOException | SQLException ex) {
                JOptionPane.showMessageDialog(this, StringUtils.capitalize("erro ao exportar o relatório: " + ex.getMessage()), StringUtils.capitalize("Erro"), JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }
}

// ==============================================================================
// 5. CLASSE PRINCIPAL (Main)
//    - Ponto de entrada da aplicação.
//    - Cria a janela principal e o controlador.
// ==============================================================================

/**
 * Classe principal que inicia a aplicação.
 * Configura a janela principal e o `JTabbedPane` para as diferentes telas.
 */
public class Main extends JFrame {
    private ERPController controller;

    public Main() {
        controller = new ERPController();

        setTitle("Mini ERP - Pedidos"); // Nome do título alterado
        setSize(1000, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Centraliza a janela

        // Fecha a conexão com o banco de dados ao fechar a janela
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent e) {
                controller.fecharConexao();
                System.out.println("Conexão com o banco de dados fechada.");
            }
        });

        // Cria as abas da interface
        JTabbedPane abas = new JTabbedPane();
        abas.addTab("Clientes", new ClientesPanel(controller));
        abas.addTab("Produtos", new ProdutosPanel(controller));
        abas.addTab("Pedidos", new PedidosPanel(controller, this));
        abas.addTab("Relatório", new RelatorioPanel(controller));

        add(abas);
    }

    /**
     * Método main, ponto de partida da aplicação.
     */
    public static void main(String[] args) {
        // Inicia a aplicação na thread de despacho de eventos (EDT) do Swing
        SwingUtilities.invokeLater(() -> {
            new Main().setVisible(true);
        });
    }
}