package com.application.vaccine_system.service;

import com.application.vaccine_system.config.security.SecurityUtil;
import com.application.vaccine_system.model.*;
import com.application.vaccine_system.model.Appointment.Status;
import com.application.vaccine_system.model.request.ReqAppointment;
import com.application.vaccine_system.model.response.Pagination;
import com.application.vaccine_system.model.response.ResAppointment;
import com.application.vaccine_system.repository.*;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppointmentService {
    private final AppointmentRepository appointmentRepository;
    private final VaccineRepository vaccineRepository;
    private final CenterRepository centerRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;

    public ResAppointment convertToReqAppointment(Appointment appointment) {
        ResAppointment res = new ResAppointment();
        res.setAppointmentId(appointment.getAppointmentId());
        res.setVaccineName(appointment.getVaccine().getName());
        res.setPatientName(appointment.getPatient().getFullname());
        res.setCenterName(appointment.getCenter().getName());
        res.setAppointmentDate(appointment.getAppointmentDate());
        res.setAppointmentTime(appointment.getAppointmentTime());
        res.setStatus(appointment.getStatus());
        return res;
    }

    public ResAppointment createAppointment(ReqAppointment reqAppointment, String paymentMethod)
            throws UnsupportedEncodingException {
        Vaccine vaccine = vaccineRepository.findById(reqAppointment.getVaccineId()).get();
        vaccine.setStockQuantity(vaccine.getStockQuantity() - 1);

        Appointment appointment = Appointment.builder()
                .vaccine(vaccineRepository.save(vaccine))
                .patient(userRepository.findById(reqAppointment.getPatientId()).get())
                .center(centerRepository.findById(reqAppointment.getCenterId()).get())
                .appointmentDate(reqAppointment.getAppointmentDate())
                .appointmentTime(reqAppointment.getAppointmentTime())
                .status(Status.PENDING).build();
        appointmentRepository.save(appointment);
        if (paymentMethod.equals("CASH")) {
            Payment payment = Payment.builder()
                    .appointment(appointment)
                    .paymentDate(LocalDate.now())
                    .amount(vaccine.getPrice())
                    .paymentMethod(Payment.PaymentMethod.CASH)
                    .status(Payment.PaymentStatus.PENDING)
                    .build();
            paymentRepository.save(payment);
        } else if (paymentMethod.equals("CREDIT_CARD")) {
            Payment payment = Payment.builder()
                    .appointment(appointment)
                    .paymentDate(LocalDate.now())
                    .amount(vaccine.getPrice())
                    .paymentMethod(Payment.PaymentMethod.CREDIT_CARD)
                    .status(Payment.PaymentStatus.COMPLETED)
                    .build();
            paymentRepository.save(payment);
        }
        // PaymentMethod.infoVNPay(vaccine.getPrice());
        ResAppointment res = convertToReqAppointment(appointment);
        return res;
    }

    public Pagination getAllAppointmentsOfCenter(Specification<Appointment> specification, Pageable pageable) {

        Page<Appointment> pageAppointment = appointmentRepository.findAll(specification, pageable);
        Pagination pagination = new Pagination();
        Pagination.Meta meta = new Pagination.Meta();

        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());

        meta.setPages(pageAppointment.getTotalPages());
        meta.setTotal(pageAppointment.getTotalElements());

        pagination.setMeta(meta);

        List<ResAppointment> listAppointments = pageAppointment.getContent()
                .stream().map(this::convertToReqAppointment)
                .collect(Collectors.toList());

        pagination.setResult(listAppointments);

        return pagination;
    }

    public ResAppointment updateAppointment(String id, ReqAppointment reqAppointment) {
        String email = SecurityUtil.getCurrentUserLogin().isPresent()
                ? SecurityUtil.getCurrentUserLogin().get()
                : "";
        User cashier = userRepository.findByEmail(email);
        User doctor = userRepository.findById(reqAppointment.getDoctorId()).get();
        Appointment appointment = appointmentRepository.findById(Long.parseLong(id)).get();
        appointment.setCashier(cashier);
        appointment.setDoctor(doctor);
        appointment.setStatus(Status.PROCESSING);
        this.appointmentRepository.save(appointment);
        return convertToReqAppointment(appointment);
    }
}
