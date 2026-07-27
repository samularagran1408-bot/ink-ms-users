package com.inklusport.users.service;

import com.inklusport.users.dto.UserProfileResponse;
import com.inklusport.users.entity.User;
import com.inklusport.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class VerificationService {

    private final UserRepository userRepository;

    /**
     * Verifica si un usuario cumple los requisitos para ser ORGANIZADOR.
     */
    @Transactional
    public UserProfileResponse verifyOrganizer(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        long days = ChronoUnit.DAYS.between(user.getCreatedAt(), LocalDateTime.now());
        user.setPlatformDays((int) days);

        boolean meetsRequirements =
                user.getEventsAttended() >= 5 &&
                user.isTestEventCreated() &&
                user.isEmailVerified() &&
                user.isPhoneVerified() &&
                days >= 30 &&
                user.isOrganizerQuizPassed();

        if (meetsRequirements) {
            user.setOrganizerVerificationStatus(User.VerificationStatus.approved);
            if (!user.getVerifiedRoles().contains("ORGANIZADOR")) {
                user.setVerifiedRoles(user.getVerifiedRoles() + ",ORGANIZADOR");
            }
            log.info("Usuario {} verificado como ORGANIZADOR", userId);
        } else {
            user.setOrganizerVerificationStatus(User.VerificationStatus.rejected);
            log.info("Usuario {} NO cumple requisitos para ORGANIZADOR", userId);
        }

        userRepository.save(user);
        return convertToResponse(user);
    }

    /**
     * Verifica si un usuario cumple los requisitos para ser ENTRENADOR.
     */
    @Transactional
    public UserProfileResponse verifyTrainer(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        boolean meetsRequirements =
                user.getCertificationFile() != null &&
                user.getExperienceMonths() >= 6 &&
                user.getEventsAsTrainer() >= 3 &&
                user.isTrainerQuizPassed() &&
                user.getIdentityDocument() != null;

        if (meetsRequirements) {
            user.setTrainerVerificationStatus(User.VerificationStatus.approved);
            if (!user.getVerifiedRoles().contains("ENTRENADOR")) {
                user.setVerifiedRoles(user.getVerifiedRoles() + ",ENTRENADOR");
            }
            log.info("Usuario {} verificado como ENTRENADOR", userId);
        } else {
            user.setTrainerVerificationStatus(User.VerificationStatus.rejected);
            log.info("Usuario {} NO cumple requisitos para ENTRENADOR", userId);
        }

        userRepository.save(user);
        return convertToResponse(user);
    }

    /**
     * Mapea entidad User a DTO de respuesta.
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
                .eventsAsTrainer(user.getEventsAsTrainer())
                .trainerQuizScore(user.getTrainerQuizScore())
                .trainerQuizPassed(user.isTrainerQuizPassed())
                .identityDocument(user.getIdentityDocument())
                .trainerVerificationStatus(
                        user.getTrainerVerificationStatus() != null ?
                        user.getTrainerVerificationStatus().name() : "pending"
                )
                .verifiedRoles(user.getVerifiedRoles())
                .build();
    }
}