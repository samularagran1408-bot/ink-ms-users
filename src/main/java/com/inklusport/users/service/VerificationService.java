package com.inklusport.users.service;

import com.inklusport.users.dto.UserProfileResponse;
import com.inklusport.users.entity.User;
import com.inklusport.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/**
 * Servicio de verificación de roles; el quiz aprobado basta para aprobar.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VerificationService {

    private final UserRepository userRepository;

    /**
     * Aprueba ORGANIZADOR cuando organizerQuizPassed es verdadero.
     */
    @Transactional
    public UserProfileResponse verifyOrganizer(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (user.isOrganizerQuizPassed()) {
            user.setOrganizerVerificationStatus(User.VerificationStatus.approved);
            if (!user.getVerifiedRoles().contains("ORGANIZADOR")) {
                user.setVerifiedRoles(user.getVerifiedRoles() + ",ORGANIZADOR");
            }
            log.info("Usuario {} verificado como ORGANIZADOR (quiz)", userId);
        } else {
            user.setOrganizerVerificationStatus(User.VerificationStatus.rejected);
            log.info("Usuario {} NO cumple requisitos para ORGANIZADOR", userId);
        }

        userRepository.save(user);
        return convertToResponse(user);
    }

    /**
     * Aprueba ENTRENADOR cuando trainerQuizPassed es verdadero.
     */
    @Transactional
    public UserProfileResponse verifyTrainer(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (user.isTrainerQuizPassed()) {
            user.setTrainerVerificationStatus(User.VerificationStatus.approved);
            if (!user.getVerifiedRoles().contains("ENTRENADOR")) {
                user.setVerifiedRoles(user.getVerifiedRoles() + ",ENTRENADOR");
            }
            log.info("Usuario {} verificado como ENTRENADOR (quiz)", userId);
        } else {
            user.setTrainerVerificationStatus(User.VerificationStatus.rejected);
            log.info("Usuario {} NO cumple requisitos para ENTRENADOR", userId);
        }

        userRepository.save(user);
        return convertToResponse(user);
    }

    /**
     * Mapea la entidad User al DTO de verificación/perfil.
     */
    private UserProfileResponse convertToResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .profilePicture(user.getProfilePicture())
                .bio(user.getBio())
                .disability(user.getDisability())
                .isActive(user.isActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .emailVerified(user.isEmailVerified())
                .phoneVerified(user.isPhoneVerified())
                .eventsAttended(user.getEventsAttended())
                .eventsCreated(user.getEventsCreated())
                .platformDays(user.getPlatformDays())
                .testEventCreated(user.isTestEventCreated())
                .organizerQuizScore(user.getOrganizerQuizScore())
                .organizerQuizPassed(user.isOrganizerQuizPassed())
                .organizerVerificationStatus(
                        user.getOrganizerVerificationStatus() != null ?
                        user.getOrganizerVerificationStatus().name() : "pending"
                )
                .certificationFile(user.getCertificationFile())
                .experienceMonths(user.getExperienceMonths())
                .experienceYears(user.getExperienceMonths() / 12)
                .eventsAsTrainer(user.getEventsAsTrainer())
                .trainerQuizScore(user.getTrainerQuizScore())
                .trainerQuizPassed(user.isTrainerQuizPassed())
                .trainerQuizAttempts(user.getTrainerQuizAttempts())
                .organizerQuizAttempts(user.getOrganizerQuizAttempts())
                .quizDisciplines(user.getQuizDisciplines())
                .identityDocument(user.getIdentityDocument())
                .trainerVerificationStatus(
                        user.getTrainerVerificationStatus() != null ?
                        user.getTrainerVerificationStatus().name() : "pending"
                )
                .verifiedRoles(user.getVerifiedRoles())
                .build();
    }
}