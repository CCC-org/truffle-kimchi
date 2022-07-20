package ccc.keewedomain.domain.nest;

import ccc.keewedomain.domain.user.Profile;

import javax.persistence.*;

import static javax.persistence.FetchType.LAZY;

@Entity
@IdClass(ChoiceId.class)
public class Choice {

    @Id
    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    @Id
    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private Profile profile;
}
