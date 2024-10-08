package com.br.library.library.service;

import com.br.library.library.domain.Book;
import com.br.library.library.domain.Reservation;
import com.br.library.library.domain.Usuario;
import com.br.library.library.dtos.reservation.ReservationDto;
import com.br.library.library.exception.BadRequestException;
import com.br.library.library.methodsToCheckThings.CheckThingsIFIsCorrect;
import com.br.library.library.repository.ReservationRepository;
import com.br.library.library.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import static com.br.library.library.enums.StatusToReserve.*;

@Service
@RequiredArgsConstructor
public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final BookService bookService;
    private final UsuarioRepository usuarioRepository;
    private final CheckThingsIFIsCorrect checkThingsIFIsCorrect;



    public Reservation findById(Long id) {
        return reservationRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Reservation not found"));
    }
    public List<Book> findBooksMostReserved(){
        return reservationRepository.findBookMostReserved();
    }

    public List<Reservation> findReservationByUsuario(String username, String password) {
        Usuario usuario = usuarioRepository.findByLogin(username
                .describeConstable()
                .orElseThrow(() -> new EntityNotFoundException("Login not found, check the field")));

        checkThingsIFIsCorrect.checkPasswordIsOk(password, usuario.getPassword());

        List<Reservation> reservationByUser = reservationRepository.findReservationByUsuarioId(usuario.getId());

        if(reservationByUser.isEmpty()) {
            throw new BadRequestException("You don't have any reservations yet");
        }
        return reservationByUser;
    }

    @Transactional
    public Reservation makeReservation(ReservationDto reservationPost) {

        Usuario userByLogin = usuarioRepository.findByLogin(reservationPost.getLogin()
                .describeConstable()
                .orElseThrow(() -> new EntityNotFoundException("Login not found, check the field")));

        Usuario userByEmail = usuarioRepository.findByEmail(reservationPost.getEmail())
                .orElseThrow(() -> new EntityNotFoundException("Email not found, check the field"));

        if(!Objects.equals(userByEmail, userByLogin)){
            throw new BadRequestException("Fields of the User aren't the corrects , check it");
        }

        checkThingsIFIsCorrect.checkPasswordIsOk(reservationPost.getPassword(), userByLogin.getPassword());
        Book book = bookService.findByTitle(reservationPost.getTitle());

        if(book.getStatusToReserve() == RESERVED || book.getStatusToReserve() == CANCELED ){
            throw new BadRequestException("Book is not available ");
        }

        book.setStatusToReserve(RESERVED);
        Reservation reservation = new Reservation(userByLogin, book);
        return reservationRepository.save(reservation);

    }

    @Transactional
    public void returnBook(ReservationDto reservationPost) {

        Book book = bookService
                .findByTitleAndGenreAndAuthor(reservationPost.getTitle(),
                        reservationPost.getGenre(), reservationPost.getAuthor());


        Usuario usuario = usuarioRepository
                .findByLoginAndEmail(reservationPost.getLogin(), reservationPost.getEmail())
                .orElseThrow(() -> new EntityNotFoundException("User not found, check the fields are corrects"));


        Reservation reservation = reservationRepository.findByBookAndUsuario(book, usuario)
                .orElseThrow(() -> new EntityNotFoundException("Reservation not found, check the user and book"));

        checkThingsIFIsCorrect.checkPasswordIsOk(reservationPost.getPassword(), usuario.getPassword());

        if(book.getStatusToReserve() == RESERVED ){
            book.setStatusToReserve(AVAILABLE);
            Reservation reservationToBeSave = new Reservation(usuario, book);
            reservationToBeSave.setId(reservation.getId());
            reservationToBeSave.setReturnDate(LocalDate.now());
            reservationRepository.save(reservationToBeSave);


        } else
            throw new BadRequestException("Do you reserved the book ? Check it , maybe you returned it");

    }



}
