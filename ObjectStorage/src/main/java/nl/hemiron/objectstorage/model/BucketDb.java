package nl.hemiron.objectstorage.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "Bucket")
@NoArgsConstructor
public class BucketDb {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Getter
    private String name;

    public BucketDb(String name) {
        this.name = name;
    }
}
