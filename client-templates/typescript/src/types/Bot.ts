export interface Bot {
    id: string,
    name: string,
    color?: string,
    position?: {
        x: number,
        y: number
    }
}