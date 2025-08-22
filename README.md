# 📦 Mini ERP em Java Puro  

Este projeto é um **sistema ERP simplificado** desenvolvido em **Java puro** com interface gráfica utilizando **Swing** e persistência em **SQLite**. O objetivo é demonstrar habilidades em **desenvolvimento backend**, organização de camadas (DAO, Controller, Models), manipulação de dados e integração com interface gráfica.

---

## 🚀 Funcionalidades

- **Cadastro de Clientes**  
  - Nome, e-mail, telefone, endereço, cidade, estado e país  
  - Listagem, edição e exclusão de registros  

- **Cadastro de Produtos**  
  - Nome, preço e ID  

- **Gestão de Pedidos**  
  - Associação de pedidos a clientes  
  - Adição de múltiplos produtos em um mesmo pedido  
  - Cálculo automático de subtotal e total  

- **Relatórios**  
  - Exportação de pedidos em formato **CSV**  

---

## 🛠️ Tecnologias Utilizadas

- **Java SE 17+**  
- **Swing** (para interface gráfica)  
- **SQLite** (persistência dos dados em banco relacional)  
- **DAO Pattern** (separação da camada de acesso a dados)  
- **Collections API** (listas de pedidos e itens)  

---

## 📂 Estrutura do Projeto

src/main/java/DIO/
│── Cliente.java # Entidade Cliente
│── Produto.java # Entidade Produto
│── Pedido.java # Entidade Pedido
│── ItemPedido.java # Associação Pedido x Produto
│── ClienteDAO.java # Persistência de Cliente
│── ProdutoDAO.java # Persistência de Produto
│── PedidoDAO.java # Persistência de Pedido
│── ERPController.java # Lógica de negócios
│── ClientesPanel.java # Tela de clientes
│── ProdutosPanel.java # Tela de produtos
│── PedidosPanel.java # Tela de pedidos
│── RelatorioPanel.java # Tela de relatórios
│── Main.java # Classe principal


---

## 📸 Demonstração  

### Cadastro de Cliente  
<img width="1366" height="768" alt="Captura de tela 2025-08-21 204227" src="https://github.com/user-attachments/assets/5ab60ee9-b7c6-43d2-95d0-ed5ef8b8470e" />

### Cadastro de Pedido  
<img width="1366" height="768" alt="Captura de tela 2025-08-21 204313" src="https://github.com/user-attachments/assets/81b3696c-1f29-4486-8b8d-05cd8ee90db5" />


### Relatórios em CSV  
<img width="1366" height="768" alt="Captura de tela 2025-08-21 204052" src="https://github.com/user-attachments/assets/29580274-bccb-45e6-8270-53512bd58787" />


📊 Veja um exemplo de relatório exportado: <img width="1253" height="684" alt="Captura de tela 2025-08-21 210625" src="https://github.com/user-attachments/assets/54c30e7a-a679-4cb5-9074-bea8057e480a" />


📚 Conceitos Demonstrados

- Programação orientada a objetos (POO) em Java

- Implementação de entidades com Serializable

- Camadas de persistência utilizando DAO Pattern

- Manipulação de listas e agregados (Pedidos com Itens)

- Exportação de dados em CSV

- Criação de uma aplicação desktop funcional

🧑‍💻 Autor

Desenvolvido por Luiz Guni
🔗[ LinkedIn](https://www.linkedin.com/in/luizguni/)

📌 Projeto criado para reforçar conceitos de backend Java com aplicação prática.
