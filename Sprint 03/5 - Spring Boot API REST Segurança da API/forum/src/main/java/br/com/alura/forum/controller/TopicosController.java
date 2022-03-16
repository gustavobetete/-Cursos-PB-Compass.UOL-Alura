package br.com.alura.forum.controller;

import br.com.alura.forum.controller.dto.DetalhesTopicoDto;
import br.com.alura.forum.controller.dto.TopicoDto;
import br.com.alura.forum.controller.form.AtualizacaoTopicoForm;
import br.com.alura.forum.controller.form.TopicoForm;
import br.com.alura.forum.modelo.Curso;
import br.com.alura.forum.modelo.Topico;
import br.com.alura.forum.repository.CursoRepository;
import br.com.alura.forum.repository.TopicoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import javax.transaction.Transactional;
import javax.validation.Valid;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@RestController //Com essa anotação, eu omito o "Response Body" de cada método. Pois o Spring assume que todos os métodos irão usá-lo.
@RequestMapping(value = "/topicos")
public class TopicosController {
    @Autowired
    private TopicoRepository topicoRepository;

    @Autowired
    private CursoRepository cursoRepository;

    @GetMapping // mesma url porem agora com metodo Get
    @Cacheable(value = "listaDeTopicos") //Para que o Spring guarde o retorno de um método no cache, devemos anotá-lo com @Cacheable;
    public Page<TopicoDto> lista(@RequestParam(required = false) String nomeCurso, @PageableDefault(sort="id", direction = Sort.Direction.DESC, page = 0, size = 10) Pageable paginacao){
                                                                                    //@PageableDefault serve para indicar o padrão de paginação/ordenação ao Spring, quando o cliente da API não enviar tais informações
        System.out.println("Nome curso");
        if(nomeCurso == null){
            Page<Topico> topicos = topicoRepository.findAll(paginacao);
            return TopicoDto.converter(topicos);
        }else {
            Page<Topico> topicos = topicoRepository.findByCursoNome(nomeCurso, paginacao);
            return TopicoDto.converter(topicos);
        }

    }
    //O Spring usa uma biblioteca chamada Jackson, o Spring pega essa lista, passa pro jackson, jackson converte pra Json e depois converteu, e devolveu como String

    @PostMapping // mesma url porem agora com metodo Post
    @Transactional
    @CacheEvict(value = "listaDeTopicos", allEntries = true) // Para o Spring invalidar algum cache após um determinado método ser chamado, devemos anotá-lo com @CacheEvict;
    public ResponseEntity<TopicoDto> cadastrar(@RequestBody @Valid TopicoForm form, UriComponentsBuilder uriBuilder){ // RequestBody indica ao String que os parâmetros enviados no corpo da requisição devem ser atribuidos ao parâmetro do método

        Topico topico = form.converter(cursoRepository);
        topicoRepository.save(topico);

        URI uri = uriBuilder.path("/topicos/{id}").buildAndExpand(topico.getId()).toUri();
        return ResponseEntity.created(uri).body(new TopicoDto(topico));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DetalhesTopicoDto> detalhar(@PathVariable Long id){

       //Topico topico = topicoRepository.getOne(id);// a partir da versão 2.5 do spring boot o método getOne(), foi marcado como deprecated, agora devemos usar o getById()
       Optional<Topico> topico = topicoRepository.findById(id);
       if(topico.isPresent()){
           return ResponseEntity.ok(new DetalhesTopicoDto(topico.get()));
       }
       return ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}") // put sobrescreve / atualiza todas informações que ja estão escritas.
    @Transactional // se não fizer o @Transactional ele não atualiza o commit no banco de dados
    @CacheEvict(value = "listaDeTopicos", allEntries = true)
    public ResponseEntity<TopicoDto> atualizar(@PathVariable Long id, @RequestBody @Valid AtualizacaoTopicoForm form ){
        Optional<Topico> optional = topicoRepository.findById(id);
        if(optional.isPresent()){
            Topico topico = form.atualizar(id, topicoRepository);
            return ResponseEntity.ok(new TopicoDto(topico));
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    @Transactional
    @CacheEvict(value = "listaDeTopicos", allEntries = true)
    public ResponseEntity<?> remover(@PathVariable Long id){
        Optional<Topico> optional = topicoRepository.findById(id);
        if(optional.isPresent()) {
            topicoRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}