#!/bin/bash
set -e

echo "Inicializando recursos AWS para testes de integração..."

# 1. Criar o bucket S3
awslocal s3api create-bucket \
    --bucket aws-integration-lab-test \
    --region sa-east-1 \
    --create-bucket-configuration LocationConstraint=sa-east-1

# 2. Criar IAM Role para os testes
awslocal iam create-role \
    --role-name aws-integration-lab-s3-test-role \
    --assume-role-policy-document '{
        "Version": "2012-10-17",
        "Statement": [{
            "Effect": "Allow",
            "Principal": {
                "AWS": "arn:aws:iam::000000000000:root"
            },
            "Action": "sts:AssumeRole"
        }]
    }'

# 3. Associar a política de permissões S3 à Role
awslocal iam put-role-policy \
    --role-name aws-integration-lab-s3-test-role \
    --policy-name aws-integration-lab-s3-test-policy \
    --policy-document '{
        "Version": "2012-10-17",
        "Statement": [
            {
                "Effect": "Allow",
                "Action": "s3:ListBucket",
                "Resource": "arn:aws:s3:::aws-integration-lab-test"
            },
            {
                "Effect": "Allow",
                "Action": [
                    "s3:PutObject",
                    "s3:GetObject",
                    "s3:DeleteObject"
                ],
                "Resource": "arn:aws:s3:::aws-integration-lab-test/*"
            }
        ]
    }'

echo "Recursos AWS de teste criados com sucesso!"
