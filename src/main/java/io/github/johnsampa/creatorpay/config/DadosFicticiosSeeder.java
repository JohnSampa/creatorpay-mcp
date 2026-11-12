package io.github.johnsampa.creatorpay.config;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import io.github.johnsampa.creatorpay.domain.MetodoPagamento;
import io.github.johnsampa.creatorpay.domain.Pedido;
import io.github.johnsampa.creatorpay.domain.Produto;
import io.github.johnsampa.creatorpay.domain.Produtor;
import io.github.johnsampa.creatorpay.repository.PedidoRepository;
import io.github.johnsampa.creatorpay.repository.ProdutoRepository;
import io.github.johnsampa.creatorpay.repository.ProdutorRepository;

/**
 * Gera dados FICTÍCIOS na inicialização: 3 produtores, 8 produtos e 400 pedidos nos últimos 60 dias.
 * A semente do Random é fixa, então os dados são sempre os mesmos (bom para testes e demonstrações).
 * Nomes e e-mails são inventados; os e-mails usam o domínio reservado ".test".
 */
@Component
@ConditionalOnProperty(name = "creatorpay.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DadosFicticiosSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DadosFicticiosSeeder.class);

    private static final int TOTAL_PEDIDOS = 400;
    private static final int DIAS_DE_HISTORICO = 60;

    private static final String[] NOMES = {
            "Ana Souza", "Bruno Lima", "Carla Mendes", "Diego Rocha", "Elisa Martins", "Felipe Costa",
            "Gabriela Alves", "Heitor Ribeiro", "Isabela Nunes", "João Pereira", "Karen Duarte", "Lucas Teixeira",
            "Marina Gomes", "Nicolas Barros", "Olívia Castro", "Pedro Henrique Dias", "Rafaela Moreira", "Samuel Freitas"
    };

    private final ProdutorRepository produtorRepository;
    private final ProdutoRepository produtoRepository;
    private final PedidoRepository pedidoRepository;
    private final Clock clock;

    public DadosFicticiosSeeder(ProdutorRepository produtorRepository, ProdutoRepository produtoRepository,
                                PedidoRepository pedidoRepository, Clock clock) {
        this.produtorRepository = produtorRepository;
        this.produtoRepository = produtoRepository;
        this.pedidoRepository = pedidoRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (produtorRepository.count() > 0) {
            return;
        }

        Produtor fitness = produtorRepository.save(new Produtor("Ana Fit", "Fitness"));
        Produtor programacao = produtorRepository.save(new Produtor("Código do Zero", "Programação"));
        Produtor culinaria = produtorRepository.save(new Produtor("Cozinha da Lu", "Culinária"));

        List<Produto> produtos = produtoRepository.saveAll(List.of(
                new Produto("Desafio 30 Dias", new BigDecimal("97.00"), fitness),
                new Produto("Plano de Treino Anual", new BigDecimal("297.00"), fitness),
                new Produto("Curso Java do Zero", new BigDecimal("397.00"), programacao),
                new Produto("Mentoria de Carreira Dev", new BigDecimal("1497.00"), programacao),
                new Produto("E-book Git na Prática", new BigDecimal("37.00"), programacao),
                new Produto("Bolos Caseiros que Vendem", new BigDecimal("147.00"), culinaria),
                new Produto("Marmitas da Semana", new BigDecimal("67.00"), culinaria),
                new Produto("Confeitaria Profissional", new BigDecimal("497.00"), culinaria)));

        Random random = new Random(42);
        Instant agora = clock.instant();
        List<Pedido> pedidos = new ArrayList<>();

        for (int i = 0; i < TOTAL_PEDIDOS; i++) {
            Produto produto = produtos.get(random.nextInt(produtos.size()));
            String nome = NOMES[random.nextInt(NOMES.length)];
            MetodoPagamento metodo = sortearMetodo(random);
            Instant criadoEm = agora.minus(Duration.ofMinutes(random.nextLong(DIAS_DE_HISTORICO * 24L * 60)));
            Instant vencimentoEm = switch (metodo) {
                case PIX -> criadoEm.plus(Duration.ofMinutes(30));
                case BOLETO -> criadoEm.plus(Duration.ofDays(3));
                case CARTAO -> null;
            };

            Pedido pedido = new Pedido("CP-" + (10001 + i), produto, nome, emailFicticio(nome, i),
                    metodo, criadoEm, vencimentoEm);
            definirStatus(pedido, random, agora);
            pedidos.add(pedido);
        }

        pedidoRepository.saveAll(pedidos);
        log.info("Dados fictícios gerados: 3 produtores, {} produtos e {} pedidos.", produtos.size(), pedidos.size());
    }

    private static MetodoPagamento sortearMetodo(Random random) {
        int r = random.nextInt(100);
        if (r < 45) {
            return MetodoPagamento.PIX;
        }
        return r < 85 ? MetodoPagamento.CARTAO : MetodoPagamento.BOLETO;
    }

    /** Distribuição aproximada de um e-commerce de infoprodutos: boleto converte menos que PIX e cartão. */
    private static void definirStatus(Pedido pedido, Random random, Instant agora) {
        int r = random.nextInt(100);
        if (r < 18) {
            pedido.marcarComoAbandonado();
            return;
        }
        int chanceDePagar = switch (pedido.getMetodo()) {
            case PIX -> 88;
            case CARTAO -> 94;
            case BOLETO -> 60;
        };
        if (random.nextInt(100) < chanceDePagar) {
            pedido.marcarComoPago(pedido.getCriadoEm().plus(Duration.ofMinutes(1 + random.nextInt(60 * 24))));
            if (pedido.getMetodo() == MetodoPagamento.CARTAO && random.nextInt(100) < 3) {
                pedido.marcarComoReembolsado();
            }
            return;
        }
        // Não pagou: se o prazo já passou, está vencido; senão, continua pendente.
        if (pedido.getVencimentoEm() != null && pedido.getVencimentoEm().isBefore(agora)) {
            pedido.marcarComoVencido();
        }
    }

    private static String emailFicticio(String nome, int indice) {
        String base = nome.toLowerCase(Locale.ROOT)
                .replace(' ', '.')
                .replace('ã', 'a').replace('í', 'i').replace('ó', 'o').replace('é', 'e').replace('á', 'a');
        return base + indice + "@email.test";
    }
}
