def getAccountID(environment) {
    switch(environment) {
        case 'development':
            return '596059882666'
        case 'qa':
            return '123456789012'
        case 'uat':
            return '123456789012'
        case 'pre-prod':
            return '123456789012'
        case 'prod':
            return '123456789012'
        default:
            error "Invalid environment: ${environment}"
    }
}
