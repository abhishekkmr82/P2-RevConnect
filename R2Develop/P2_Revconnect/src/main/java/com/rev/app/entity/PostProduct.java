package com.rev.app.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "post_products")
public class PostProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "post_products_seq")
    @SequenceGenerator(name = "post_products_seq", sequenceName = "post_products_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    public PostProduct() {
    }

    public PostProduct(Post post, Product product) {
        this.post = post;
        this.product = product;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Post getPost() {
        return post;
    }

    public void setPost(Post post) {
        this.post = post;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }
}
